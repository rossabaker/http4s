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
import scalaz.\/
import scalaz.concurrent.Task

/**
 * Created by Bryce Anderson on 6/24/14.
 */

class BlazeClientStage(implicit val ec: ExecutionContext)
        extends Http1ClientParser with TailStage[ByteBuffer] with Http1Stage {

  private type Callback = Throwable\/Response => Unit

  private var status: Status = null
  private var headers = new ListBuffer[Header]

  override def name: String = "BlazeClientStage"


  def runRequest(req: Request): Task[Response] = Task.async { cb =>
    val reqline = makeRequestLine(req)
    val h = writeHeaders(req.headers)

    val writer =

    writeBody(Vector(reqline, h), req.body, cb)
  }

  override protected def submitResponseLine(code: Int, reason: String,
                                  scheme: String,
                                  majorversion: Int, minorversion: Int): Unit = {
    status = Status(code)
  }

  override protected def headerComplete(name: String, value: String): Boolean = {
    headers += Header(name, value)
    false
  }


  ///////////////////////// Private helpers /////////////////////////

  private def writeBody(prev: Seq[ByteBuffer], body: HttpBody, cb: Callback) {

    ???
  }

  private def getWriter(req: Request, rr: StringWriter): ProcessWriter = {
    val minor = req.protocol match {
      case HttpVersion(_, minor) => minor
      case p => sys.error(s"Don't know the server protocol: $p")
    }
    val closeOnFinish = true  // TODO: we might want a client that stays alive

    getEncoder(req, rr, minor, closeOnFinish)
  }

  private def makeRequestLine(req: Request): ByteBuffer = ???
}


