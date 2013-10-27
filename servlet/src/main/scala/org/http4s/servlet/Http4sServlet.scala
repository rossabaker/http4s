package org.http4s
package servlet

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import play.api.libs.iteratee._
import java.net.InetAddress
import scala.collection.JavaConverters._
import scala.concurrent.{Promise, ExecutionContext, Future}
import javax.servlet._
import org.http4s.Status.{InternalServerError, NotFound}
import akka.util.ByteString

import Http4sServlet._
import scala.util.logging.Logged
import com.typesafe.scalalogging.slf4j.Logging
import scala.annotation.tailrec
import scala.util.{Failure, Success}
import scala.util.Failure
import scala.util.Success
import org.http4s.TrailerChunk
import org.http4s.TrailerChunk

class Http4sServlet(route: Route, chunkSize: Int = DefaultChunkSize)
                   (implicit executor: ExecutionContext = ExecutionContext.global) extends HttpServlet with Logging {
  private[this] var serverSoftware: ServerSoftware = _

  override def init(config: ServletConfig) {
    serverSoftware = ServerSoftware(config.getServletContext.getServerInfo)
  }

  override def service(servletRequest: HttpServletRequest, servletResponse: HttpServletResponse) {
    servletRequest.getInputStream
    val request = toRequest(servletRequest)
    val ctx = servletRequest.startAsync()
          handle(request, ctx)
  }

  protected def handle(request: RequestPrelude, ctx: AsyncContext) {
    val servletRequest = ctx.getRequest.asInstanceOf[HttpServletRequest]
    val servletResponse = ctx.getResponse.asInstanceOf[HttpServletResponse]

    val parser = try {
      route.lift(request).getOrElse(Done(NotFound(request)))
    } catch { case t: Throwable => Done[HttpChunk, Responder](InternalServerError(t)) }
    val handler = parser.flatMap { responder =>
      servletResponse.setStatus(responder.prelude.status.code, responder.prelude.status.reason)
      for (header <- responder.prelude.headers)
        servletResponse.addHeader(header.name, header.value)
      import HttpEncodings.chunked

      val isChunked =
        responder.prelude.headers.get(HttpHeaders.TransferEncoding).map(_.coding.matches(chunked)).getOrElse(false)

      val writer = new OutputWriter(servletResponse.getOutputStream)
      writer.isChunked = isChunked

//      responder.body.transform(Iteratee.foreach {
//        case BodyChunk(chunk) =>
//          val out = servletResponse.getOutputStream
//          out.write(chunk.toArray)
//          if(isChunked) out.flush()
//        case t: TrailerChunk =>
//          log("The servlet adapter does not implement trailers. Silently ignoring.")
//      })
      responder.body.transform(writer)
    }

    servletRequest.getInputStream.setReadListener(new InputHandler(handler, ctx))
  }


  private class OutputWriter(os: ServletOutputStream)
                                  extends WriteListener with Iteratee[HttpChunk, Unit] {

    var isChunked = false
    private var isFirst = true
    private var p: Promise[AnyRef] = null
    private var f: (Step[HttpChunk, Unit] => Future[_]) = null

    def fold[B](folder: (Step[HttpChunk, Unit]) => Future[B])(implicit ec: ExecutionContext): Future[B] = {
      assert(f == null && p == null)

      f = folder
      p = Promise[AnyRef]

      // set the callback
      if (isFirst) {
        isFirst = false
        os.setWriteListener(this)
      } else onWritePossible()

      p.future.asInstanceOf[Future[B]]
    }

    def onError(t: Throwable) {
      // Close down

      logger.debug("Error in WriteHandler callback.", t)
      if (p != null) p.failure(t)
    }

    def onWritePossible(): Unit = if (os.isReady) {
      val ff = f
      f = null
      val result = ff(Step.Cont{
        case Input.El(chunk:BodyChunk) =>
          os.write(chunk.toArray)
          if (isChunked) os.flush()
          this

        case c: TrailerChunk =>
          logger.warn("Trailer chunks not supported. Dropped.")
          this

        case Input.Empty => this

        case Input.EOF =>
          Done((), Input.Empty)
      })

      // complete the future and get things ready for the next write
      val pp = p
      p = null
      pp.completeWith(result.asInstanceOf[Future[AnyRef]])
    }
  }

  protected def toRequest(req: HttpServletRequest): RequestPrelude = {
    RequestPrelude(
      requestMethod = Method(req.getMethod),
      scriptName = req.getContextPath + req.getServletPath,
      pathInfo = Option(req.getPathInfo).getOrElse(""),
      queryString = Option(req.getQueryString).getOrElse(""),
      protocol = ServerProtocol(req.getProtocol),
      headers = toHeaders(req),
      urlScheme = HttpUrlScheme(req.getScheme),
      serverName = req.getServerName,
      serverPort = req.getServerPort,
      serverSoftware = serverSoftware,
      remote = InetAddress.getByName(req.getRemoteAddr) // TODO using remoteName would trigger a lookup
    )
  }

  protected def toHeaders(req: HttpServletRequest): HttpHeaders = {
    val headers = for {
      name <- req.getHeaderNames.asScala
      value <- req.getHeaders(name).asScala
    } yield HttpHeaders.RawHeader(name, value)
    HttpHeaders(headers.toSeq : _*)
  }

  private class InputHandler(handler: Iteratee[HttpChunk, Unit], ctx: AsyncContext) extends ReadListener {

    private val is = ctx.getRequest.asInstanceOf[HttpServletRequest].getInputStream
    private val buffer = new Array[Byte](chunkSize)
    private var it = handler

    def onError(t: Throwable) { it.run.onComplete(_ => ctx.complete()) }

    def onAllDataRead() {it.run.onComplete( _ => ctx.complete()) }

    def onDataAvailable() {

      // TODO: isRead() is what triggers the reinsertion of this callback, at lest in Jetty 9.1 RC1
      // http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/jetty-servlets/src/main/java/org/eclipse/jetty/servlets/DataRateLimitedServlet.java?h=jetty-9.1#n219
      // line 286
      if (is.isReady()) {
        val remaining = is.available()
        assert( remaining > 0)
        val size = if (remaining > chunkSize) chunkSize else remaining
        val c = is.read(buffer, 0, size)  // this should always be larger than 0 if we got this far.
        val chunk = BodyChunk.fromArray(buffer, 0, c)
        it.fold {
          case Step.Cont(k) =>
            it = k(Input.El(chunk))
            onDataAvailable()
            Future.successful(Unit)

          case Step.Done(_, _) =>
            ctx.complete()
            Future.successful(Unit)

          case Step.Error(e, _) =>
            ctx.complete()
            logger.debug(s"Error during route: $e")
            Future.successful(Unit)
        }
      }
    }
  }
}

object Http4sServlet {
  private[servlet] val DefaultChunkSize = Http4sConfig.getInt("org.http4s.servlet.default-chunk-size")
}