package org.http4s
package blaze
package client

import java.nio.ByteBuffer

import http.http_parser.Http1ClientParser
import org.http4s.util.StringWriter
import pipeline.TailStage

import scala.collection.mutable.ListBuffer
import scalaz.\/
import scalaz.concurrent.Task

/**
 * Created by Bryce Anderson on 6/24/14.
 */
class BlazeClientStage extends Http1ClientParser with TailStage[ByteBuffer] {

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

  }

  private def getWriter(h: Headers): ProcessWriter = {
    ???
  }

  private def makeRequestLine(req: Request): ByteBuffer = ???

  private def writeHeaders(headers: Headers): ByteBuffer = {
    val buff = new StringWriter(512)
    headers.foreach{ h => buff ~ h.name ~ ':' ~ ' ' ~ h ~ '\r' ~ '\n' }
    buff ~ '\r' ~ '\n'  // Signal end of headers

    ByteBuffer.wrap(buff.result().getBytes(CharacterSet.`US-ASCII`.charset))
  }
}


