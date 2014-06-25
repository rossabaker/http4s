package org.http4s.blaze

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import com.typesafe.scalalogging.Logging
import org.http4s.Header.`Transfer-Encoding`
import org.http4s.{Message, Headers, Header}
import org.http4s.blaze.pipeline.TailStage
import org.http4s.util.StringWriter

import scala.concurrent.ExecutionContext
import scalaz.concurrent.Task

/**
 * Created by Bryce Anderson on 6/24/14.
 */
trait Http1Stage { self: Logging with TailStage[ByteBuffer] =>

  protected implicit def ec: ExecutionContext
  
  protected def encodeHeaders(headers: Headers, rr: StringWriter): Unit = {
    headers.foreach( header => if (header.name != `Transfer-Encoding`.name) rr ~ header ~ '\r' ~ '\n' )
  }

  /** Check Connection header and add applicable headers to response */
  protected def checkConnection(conn: Header.Connection, rr: StringWriter): Boolean = {
    if (conn.hasKeepAlive) {                          // connection, look to the request
      logger.trace("Found Keep-Alive header")
      false
    }
    else if (conn.hasClose) {
      logger.trace("Found Connection:Close header")
      rr ~ "Connection:close\r\n"
      true
    }
    else {
      logger.info(s"Unknown connection header: '${conn.value}'. Closing connection upon completion.")
      rr ~ "Connection:close\r\n"
      true
    }
  }

  /** Get the proper body encoder based on the message headers */
  final protected def getEncoder(msg: Message,
                                 rr: StringWriter,
                                 minor: Int,
                                 closeOnFinish: Boolean): ProcessWriter = {
    val headers = msg.headers
    getEncoder(Header.Connection.from(headers),
               Header.`Transfer-Encoding`.from(headers),
               Header.`Content-Length`.from(headers),
               msg.trailerHeaders,
               rr,
               minor,
               closeOnFinish)
  }

  /** Get the proper body encoder based on the message headers */
  protected def getEncoder(connectionHeader: Option[Header.Connection],
                 bodyEncoding: Option[Header.`Transfer-Encoding`],
                 lengthHeader: Option[Header.`Content-Length`],
                 trailer: Task[Headers],
                 rr: StringWriter,
                 minor: Int,
                 closeOnFinish: Boolean): ProcessWriter = lengthHeader match {
    case Some(h) if bodyEncoding.isEmpty =>
      logger.trace("Using static encoder")

      // add KeepAlive to Http 1.0 responses if the header isn't already present
      if (!closeOnFinish && minor == 0 && connectionHeader.isEmpty) rr ~ "Connection:keep-alive\r\n\r\n"
      else rr ~ '\r' ~ '\n'

      val b = ByteBuffer.wrap(rr.result().getBytes(StandardCharsets.US_ASCII))
      new StaticWriter(b, h.length, this)

    case _ =>  // No Length designated for body or Transfer-Encoding included
      if (minor == 0) { // we are replying to a HTTP 1.0 request see if the length is reasonable
        if (closeOnFinish) {  // HTTP 1.0 uses a static encoder
          logger.trace("Using static encoder")
          rr ~ '\r' ~ '\n'
          val b = ByteBuffer.wrap(rr.result().getBytes(StandardCharsets.US_ASCII))
          new StaticWriter(b, -1, this)
        }
        else {  // HTTP 1.0, but request was Keep-Alive.
          logger.trace("Using static encoder without length")
          new CachingStaticWriter(rr, this) // will cache for a bit, then signal close if the body is long
        }
      }
      else {
        rr ~ "Transfer-Encoding: chunked\r\n\r\n"
        val b = ByteBuffer.wrap(rr.result().getBytes(StandardCharsets.US_ASCII))

        bodyEncoding match { // HTTP >= 1.1 request without length. Will use a chunked encoder
          case Some(h) => // Signaling chunked may mean flush every chunk
            if (!h.hasChunked) logger.warn(s"Unknown transfer encoding: '${h.value}'. Defaulting to Chunked Encoding")
            new ChunkProcessWriter(b, this, trailer)

          case None =>     // use a cached chunk encoder for HTTP/1.1 without length of transfer encoding
            logger.trace("Using Caching Chunk Encoder")
            new CachingChunkWriter(b, this, trailer)
        }
      }
  }

}
