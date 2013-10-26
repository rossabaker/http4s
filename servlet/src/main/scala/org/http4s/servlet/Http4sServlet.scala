package org.http4s
package servlet

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import play.api.libs.iteratee._
import java.net.InetAddress
import scala.collection.JavaConverters._
import concurrent.{ExecutionContext,Future}
import javax.servlet.{ServletInputStream, ReadListener, ServletConfig, AsyncContext}
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
//    executor.execute { // This shouldn't be necessary anymore, the rest of the call just builds the iteratee, and sets the input callback. Nothing can block.
//      new Runnable {
//        def run() {
          handle(request, ctx)
//        }
//      }
//    }
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

      responder.body.transform(Iteratee.foreach {
        case BodyChunk(chunk) =>
          val out = servletResponse.getOutputStream
          out.write(chunk.toArray)
          if(isChunked) out.flush()
        case t: TrailerChunk =>
          log("The servlet adapter does not implement trailers. Silently ignoring.")
      })
    }

    servletRequest.getInputStream.setReadListener(new InputHandler(handler, ctx))
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

    def onError(t: Throwable) {
      it.run.onComplete(_ => ctx.complete())
    }

    def onDataAvailable() {

      // TODO: isRead() is what triggers the reinsertion of this callback, at lest in Jetty
      // http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/jetty-servlets/src/main/java/org/eclipse/jetty/servlets/DataRateLimitedServlet.java?h=jetty-9.1#n219
      // line 286
      if (is.isReady()) {
        def go(goit: Iteratee[HttpChunk, Unit], size: Int): Unit = {
          goit.fold {
            case Step.Cont(k) =>
              val c = is.read(buffer, 0, size)  // this should always be larger than 0 if we got this far.
              val chunk = BodyChunk.fromArray(buffer, 0, c)
              val nit = k(Input.El(chunk))
              val remaining = is.available()
              if (remaining != 0) {
                go(nit, if (chunkSize < remaining) chunkSize else remaining) // Limit to size or whatever is left
              } else {
                it = nit          // Set our iteratee in position for the next call from the server thread
                onDataAvailable() // reset the callback
              }
              Future.successful(Unit)

            case Step.Done(_, _) =>
              ctx.complete()
              Future.successful(Unit)

            case Step.Error(e, _) =>
              ctx.complete()
              log(s"Error during route: $e")
              Future.successful(Unit)
          }
        }
        val remaining = is.available()
        assert( remaining > 0)
        go(it, if (chunkSize < remaining) chunkSize else remaining)
      }
    }

    def onAllDataRead() {
      it.run.onComplete( _ => ctx.complete())
    }
  }
}

object Http4sServlet {
  private[servlet] val DefaultChunkSize = Http4sConfig.getInt("org.http4s.servlet.default-chunk-size")
}