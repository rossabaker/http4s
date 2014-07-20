package org.http4s.json

import jawn.{AsyncParser, Facade}
import scodec.bits.ByteVector
import scalaz.concurrent.Task
import scalaz.stream.{Channel, Process, io}

trait JawnJsonReader[J] extends JsonReader[J] {
  protected implicit def jawnFacade: Facade[J]

  def jsonR(mode: AsyncParser.Mode): Channel[Task, Option[ByteVector], Seq[J]] = {
    val acquire = Task.delay(AsyncParser[J](mode))
    def flush(p: AsyncParser[J]) = Task.delay(p.finish().fold(throw _, identity))
    def step(p: AsyncParser[J]) = Task.now { bytes: ByteVector => Task.delay {
      p.absorb(bytes.toByteBuffer).fold(throw _, identity)
    }}
    io.bufferedChannel(acquire)(flush)(_ => Task.now(()))(step)
  }

  private def throughJsonR(p: Process[Task, ByteVector], mode: AsyncParser.Mode) =
    p.throughOption(jsonR(mode)).flatMap(Process.emitAll)

  override def parseJson(p: Process[Task, ByteVector]): Task[J] =
    throughJsonR(p, AsyncParser.SingleValue).runLastOr(jawnFacade.jnull())

  override def parseJsonArray(p: Process[Task, ByteVector]): Process[Task, J] =
    throughJsonR(p, AsyncParser.UnwrapArray)

  override def parseJsonStream(p: Process[Task, ByteVector]): Process[Task, J] =
    throughJsonR(p, AsyncParser.ValueStream)
}

