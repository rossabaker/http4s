package org.http4s
package servlet

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import play.api.libs.iteratee._
import java.net.InetAddress
import scala.collection.JavaConverters._
import concurrent.{ExecutionContext,Future}
import javax.servlet.{ReadListener, ServletConfig, AsyncContext}
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
    handle(request, ctx)
//    executor.execute {
//      new Runnable {
//        def run() {
//          handle(request, ctx)
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

    val is = servletRequest.getInputStream
    is.setReadListener(
      new ReadListener {
        private val buffer = new Array[Byte](chunkSize)
        private var fit: Future[Iteratee[HttpChunk, Unit]] = Future.successful(handler)

        def onError(t: Throwable) {
          fit.onComplete {
            case Success(it) => it.run.onComplete(_ => ctx.complete())
            case Failure(_)  => ctx.complete()
          }
        }

        def onDataAvailable() {
          var canceled = false
          def go(): Unit = {
            // TODO: isRead() is what triggers the reinsertion of this callback, at lest in Jetty
            // http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/jetty-servlets/src/main/java/org/eclipse/jetty/servlets/DataRateLimitedServlet.java?h=jetty-9.1#n219
            // line 286
            if (!canceled && is.isReady()) {
              val c = is.read(buffer, 0, chunkSize)
              if (c != -1) {
                val chunk = BodyChunk.fromArray(buffer, 0, c)
                fit = fit.flatMap( it => it.fold {
                  case Step.Cont(k) =>    Future.successful(k(Input.El(chunk)))
                  case Step.Done(_, _) =>
                    canceled = true
                    ctx.complete()
                    Future.successful(it)

                  case Step.Error(e, _) =>
                    canceled = true
                    ctx.complete()
                    log(s"Error during route: $e")
                    Future.successful(it)
                })
                go()
              }
            }
          }
          go()
        }

        def onAllDataRead() {
          fit.onComplete {
            case Success(it) => it.run.onComplete( _ => ctx.complete() )
            case Failure(_)  => ctx.complete()
          }

        }
      }
    )
//    Enumerator.fromStream(servletRequest.getInputStream, chunkSize)
//      .map[HttpChunk](BodyChunk(_))
//      .run(handler)
//      .onComplete(_ => ctx.complete() )
//    var canceled = false
//    Concurrent.unicast[HttpChunk]({
//      channel =>
//        val bytes = new Array[Byte](chunkSize)
//        val is = servletRequest.getInputStream
//
//        @tailrec
//        def push(): Unit = {
//          if(is.isReady && !canceled) {
//            val readBytes = is.read(bytes, 0, chunkSize)
//            if (readBytes != -1) {
//              channel.push(BodyChunk.fromArray(bytes, 0, readBytes))
//              push()
//            }
//          }
//        }
//        is.setReadListener( new ReadListener {
//          def onDataAvailable() { push() }
//          def onError(t: Throwable) {}
//          def onAllDataRead() { channel.eofAndEnd() }
//        })
//    },
//    {canceled = true}
//    ).run(handler)
//      .onComplete{ _ => ctx.complete() }
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
}

object Http4sServlet {
  private[servlet] val DefaultChunkSize = Http4sConfig.getInt("org.http4s.servlet.default-chunk-size")
}