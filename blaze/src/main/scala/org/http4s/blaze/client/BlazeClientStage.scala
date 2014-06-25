package org.http4s
package blaze
package client

import java.nio.ByteBuffer

import http.http_parser.Http1ClientParser
import org.http4s.ServerProtocol.HttpVersion
import org.http4s.util.{Writer, StringWriter}
import pipeline.TailStage

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scalaz.{\/-, -\/, \/}
import scalaz.concurrent.Task

/**
 * Created by Bryce Anderson on 6/24/14.
 */

class BlazeClientStage(closeOnFinish: Boolean)(implicit protected val ec: ExecutionContext)
                      extends Http1ClientParser
                      with TailStage[ByteBuffer]
                      with Http1Stage
                      with Http1ClientReceiver {

  protected type Callback = Throwable\/Response => Unit

  override def name: String = "BlazeClientStage"

  def runRequest(req: Request): Task[Response] = Task.async { cb =>
    val rr = new StringWriter(512)
    encodeRequestLine(req, rr)
    encodeHeaders(req.headers, rr)

    val closeOnFinish = Header.Connection.from(req.headers)
        .map(checkCloseConnection(_, rr))
        .getOrElse(getHttpMinor(req) == 0)

    val enc = getChunkEncoder(req, closeOnFinish, rr)

    enc.writeProcess(req.body).runAsync {
      case \/-(_)    => receiveResponse(cb)
      case e@ -\/(t) => cb(e)
    }
  }

  ///////////////////////// Private helpers /////////////////////////

  private def getHttpMinor(req: Request): Int = req.protocol match {
    case HttpVersion(_, minor) => minor
    case p => sys.error(s"Don't know the server protocol: $p")  // TODO: future proof?
  }

  private def getChunkEncoder(req: Request, closeOnFinish: Boolean, rr: StringWriter): ProcessWriter = {
    getEncoder(req, rr, getHttpMinor(req), closeOnFinish)
  }

  private def encodeRequestLine(req: Request, writer: Writer): writer.type = ???
}


