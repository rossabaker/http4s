package org.http4s
package blaze
package util

import cats.effect.IO
import scala.concurrent._
import scala.util._
import fs2._
import fs2.Stream._
import org.http4s.batteries._

trait EntityBodyWriter {

  /** The `ExecutionContext` on which to run computations, assumed to be stack safe. */
  implicit protected def ec: ExecutionContext
  implicit val strategy : Strategy = Strategy.fromExecutionContext(ec)

  /** Write a ByteVector to the wire.
    * If a request is cancelled, or the stream is closed this method should
    * return a failed Future with Cancelled as the exception
    *
    * @param chunk BodyChunk to write to wire
    * @return a future letting you know when its safe to continue
    */
  protected def writeBodyChunk(chunk: Chunk[Byte], flush: Boolean): Future[Unit]

  /** Write the ending chunk and, in chunked encoding, a trailer to the
    * wire.  If a request is cancelled, or the stream is closed this
    * method should return a failed Future with Cancelled as the
    * exception, or a Future with a Boolean to indicate whether the
    * connection is to be closed or not.
    *
    * @param chunk BodyChunk to write to wire
    * @return a future letting you know when its safe to continue (if `false`) or
    * to close the connection (if `true`)
    */
  protected def writeEnd(chunk: Chunk[Byte]): Future[Boolean]

  /** Called in the event of an Await failure to alert the pipeline to cleanup */
  protected def exceptionFlush(): Future[Unit] = Future.successful(())

  /** Creates a IO that writes the contents of the EntityBody to the output.
    * Cancelled exceptions fall through to the IO cb
    * The writeBodyEnd triggers if there are no exceptions, and the result will
    * be the result of the writeEnd call.
    *
    * @param p EntityBody to write out
    * @return the IO which when run will unwind the Process
    */
  def writeEntityBody(p: EntityBody): IO[Boolean] = {
    val writeBody : IO[Unit] = (p to writeSink).run
    val writeBodyEnd : IO[Boolean] = IO.fromFuture(writeEnd(Chunk.empty))
    writeBody >> writeBodyEnd
  }
  
  /** Writes each of the body chunks, if the write fails it returns
    * the failed future which throws an error.
    * If it errors the error stream becomes the stream, which performs an
    * exception flush and then the stream fails.
    */
  private val writeSink: Sink[IO, Byte] = { s =>
    val writeStream : Stream[IO, Unit] = s.chunks.evalMap[IO, IO, Unit](chunk =>
      IO.fromFuture(writeBodyChunk(chunk , false)))
    val errorStream : Throwable => Stream[IO, Unit] = e =>
      Stream.eval(IO.fromFuture(exceptionFlush())).flatMap{_ => fail(e)}
    writeStream.onError(errorStream)
  }
}
