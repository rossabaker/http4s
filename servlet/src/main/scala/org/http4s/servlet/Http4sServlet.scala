package org.http4s
package servlet

import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import java.net.InetAddress
import scala.collection.JavaConverters._
import javax.servlet.{AsyncEvent, AsyncListener, ServletConfig, AsyncContext}

import Http4sServlet._
import util.CaseInsensitiveString._

import scala.concurrent.duration.Duration
import scalaz.concurrent.Task
import scalaz.stream.Process
import scalaz.stream.io._
import scalaz.{-\/, \/-}
import scala.util.control.NonFatal
import org.parboiled2.ParseError
import com.typesafe.scalalogging.slf4j.LazyLogging

class Http4sServlet(service: HttpService, asyncTimeout: Duration = Duration.Inf, chunkSize: Int = DefaultChunkSize)
  extends HttpServlet with LazyLogging {

  private val asyncTimeoutMillis = if (asyncTimeout.isFinite) asyncTimeout.toMillis else -1  // -1 == Inf

  private[this] var serverSoftware: ServerSoftware = _

  override def init(config: ServletConfig) {
    serverSoftware = ServerSoftware(config.getServletContext.getServerInfo)
  }

  override def service(servletRequest: HttpServletRequest, servletResponse: HttpServletResponse) {
    try handle(servletRequest)
    catch {
      case NonFatal(e) => handleError(e, servletResponse)
    }
  }

  private def handleError(t: Throwable, response: HttpServletResponse) {
    if (!response.isCommitted) t match {
      case ParseError(_, _) =>
        logger.info("Error during processing phase of request", t)
        response.sendError(HttpServletResponse.SC_BAD_REQUEST)

      case _ =>
        logger.error("Error processing request", t)
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
    }
    else logger.error("Error processing request", t)

  }

  private def handle(servletRequest: HttpServletRequest): Unit = {
    val request = toRequest(servletRequest)
    val completed = new AtomicBoolean(false)
    val ctx = servletRequest.startAsync()
    ctx.setTimeout(asyncTimeoutMillis)
    ctx.addListener(asyncListener(completed))
    val response = invokeService(request)
    render(response, ctx, completed)
  }

  private def asyncListener(completed: AtomicBoolean) = new AsyncListener {
    override def onComplete(event: AsyncEvent): Unit = {}

    override def onError(event: AsyncEvent): Unit = {
      val ctx = event.getAsyncContext
      val req = ctx.getRequest.asInstanceOf[HttpServletRequest]
      val t = event.getThrowable
      logger.error(s"Error handling request: ${req.getMethod} ${req.getRequestURI}", t)
      sendInternalServerError(ctx)
    }

    override def onStartAsync(event: AsyncEvent): Unit = {}

    override def onTimeout(event: AsyncEvent): Unit = {
      val ctx = event.getAsyncContext
      val req = ctx.getRequest.asInstanceOf[HttpServletRequest]
      logger.error(s"Timeout handling request: ${req.getMethod} ${req.getRequestURI}")
      sendInternalServerError(ctx)
    }

    private def sendInternalServerError(ctx: AsyncContext): Unit = {
      val servletResponse = ctx.getResponse.asInstanceOf[HttpServletResponse]
      if (!servletResponse.isCommitted) {
        servletResponse.reset()
        servletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
      }
      complete(ctx, completed)
    }
  }

  private def complete(ctx: AsyncContext, completed: AtomicBoolean) = {
    if (completed.compareAndSet(false, true))
      ctx.complete()
  }

  private def invokeService(request: Request) = service.applyOrElse(request, Status.NotFound.apply)

  private def render(responseTask: Task[Response], ctx: AsyncContext, completed: AtomicBoolean) = {
    import Process._
    val servletResponse = ctx.getResponse.asInstanceOf[HttpServletResponse]
    responseTask.flatMap { response =>
      val process = eval_(renderHeader(response, servletResponse)) ++ renderBody(response, servletResponse)
      Task.fork(process.until(repeatEval(Task.delay(completed.get))).run)
    }.runAsync {
      case \/-(_) => ctx.complete()
      case -\/(t) => throw (t)
    }
  }

  private def renderHeader(response: Response, servletResponse: HttpServletResponse) = Task.delay {
    servletResponse.setStatus(response.status.code, response.status.reason)
    for (header <- response.headers)
      servletResponse.addHeader(header.name.toString, header.value)
  }

  private def renderBody(response: Response, servletResponse: HttpServletResponse) = {
    val out = servletResponse.getOutputStream
    val isChunked = response.isChunked
    response.body.map { chunk =>
      out.write(chunk.toArray)
      if (isChunked) servletResponse.flushBuffer()
    }
  }

  protected def toRequest(req: HttpServletRequest): Request =
    Request(
      requestMethod = Method.getOrElse(req.getMethod, Method.fromKey(req.getMethod)),
      requestUri = Uri.fromString(req.getRequestURI).get,
      protocol = ServerProtocol.getOrElseCreate(req.getProtocol.ci),
      headers = toHeaders(req),
      body = chunkR(req.getInputStream).map(f => f(chunkSize)).eval,
      attributes = AttributeMap(
        Request.Keys.PathInfoCaret(req.getServletPath.length),
        Request.Keys.Remote(InetAddress.getByName(req.getRemoteAddr)),
        Request.Keys.ServerSoftware(serverSoftware)
      )
    )

  protected def toHeaders(req: HttpServletRequest): Headers = {
    val headers = for {
      name <- req.getHeaderNames.asScala
      value <- req.getHeaders(name).asScala
    } yield Header(name, value)
    Headers(headers.toSeq : _*)
  }
}

object Http4sServlet {
  private[servlet] val DefaultChunkSize = Http4sConfig.getInt("org.http4s.servlet.default-chunk-size")
}