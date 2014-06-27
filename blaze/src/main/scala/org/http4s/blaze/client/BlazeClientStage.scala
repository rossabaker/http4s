package org.http4s
package blaze
package client

import java.nio.ByteBuffer

import http.http_parser.Http1ClientParser
import org.http4s.Header.Host
import org.http4s.ServerProtocol.HttpVersion
import org.http4s.blaze.http.http_parser.BaseExceptions.ParserException
import org.http4s.util.{Writer, StringWriter}
import org.http4s.blaze.pipeline.{Command, TailStage}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scalaz.{\/-, -\/, \/}
import scalaz.concurrent.Task

/**
 * Created by Bryce Anderson on 6/24/14.
 */

class BlazeClientStage(protected val closeOnFinish: Boolean,
                       protected val timeout: Duration = 60.seconds)
                      (implicit protected val ec: ExecutionContext)
                      extends Http1ClientReceiver with Http1Stage[Response] {

  protected type Callback = Throwable\/Response => Unit

  override def name: String = "BlazeClientStage"

  // TODO: Its stupid that I have to have these methods
  override protected def parserContentComplete(): Boolean = contentComplete()

  override protected def doParseContent(buffer: ByteBuffer): ByteBuffer = parseContent(buffer)

  def runRequest(req: Request): Task[Response] = Task.async { cb =>
    try {
      val rr = new StringWriter(512)
      encodeRequestLine(req, rr)
      encodeHeaders(req.headers, rr)

      logger.error("Status line and headers ---------------\n" + rr.result() + "\n------------------------")

      val closeHeader = closeOnFinish || Header.Connection.from(req.headers)
                                           .map(checkCloseConnection(_, rr))
                                           .getOrElse(getHttpMinor(req) == 0)

      val enc = getChunkEncoder(req, closeHeader, rr)

      enc.writeProcess(req.body).runAsync {
        case \/-(_)    => receiveResponse(cb)
        case e@ -\/(t) => cb(e)
      }
    } catch { case t: Throwable =>
      logger.error("Error during request submission", t)
      cb(-\/(t))
    }
  }

  def shutdown(): Task[Unit] = Task { sendOutboundCommand(Command.Disconnect) }

  ///////////////////////// Private helpers /////////////////////////

  private def getHttpMinor(req: Request): Int = req.protocol match {
    case HttpVersion(_, minor) => minor
    case p => sys.error(s"Don't know the server protocol: $p")
  }

  private def getChunkEncoder(req: Request, closeHeader: Boolean, rr: StringWriter): ProcessWriter = {
    getEncoder(req, rr, getHttpMinor(req), closeHeader)
  }

  private def encodeRequestLine(req: Request, writer: Writer): writer.type = {
    val uri = req.requestUri
    writer ~ req.requestMethod ~ ' ' ~ uri.path ~ ' ' ~ req.protocol ~ '\r' ~ '\n'
    if (getHttpMinor(req) == 1 && Host.from(req.headers).isEmpty) { // need to add the host header for HTTP/1.1
      uri.host match {
        case Some(host) =>
          writer ~ "Host: " ~ host
          if (uri.port.isDefined)  writer ~ ':' ~ uri.port.get
          writer ~ '\r' ~ '\n'

        case None =>
      }
      writer
    } else sys.error("Request URI must have a host.") // TODO: do we want to do this by exception?
  }
}


