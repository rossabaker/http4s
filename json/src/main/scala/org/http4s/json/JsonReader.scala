package org.http4s.json

import jawn.{Facade, AsyncParser}
import scodec.bits.ByteVector
import scalaz.concurrent.Task
import scalaz.stream.{Channel, io, Process}
import scalaz.stream.Process._

trait JsonReader[J] {
  def parseJson(p: Process[Task, ByteVector]): Task[J]
  def parseJsonStream(p: Process[Task, ByteVector]): Process[Task, Seq[J]]
  def parseJsonArray(p: Process[Task, ByteVector]): Process[Task, Seq[J]]

  implicit class JsonSourceSyntax(self: Process[Task, ByteVector]) {
    def parseJson: Task[J] = JsonReader.this.parseJson(self)
    def parseJsonStream: Process[Task, Seq[J]] = JsonReader.this.parseJsonStream(self)
    def parseJsonArray: Process[Task, Seq[J]] = JsonReader.this.parseJsonArray(self)
  }
}

abstract class JawnJsonReader[J](implicit facade: Facade[J]) extends JsonReader[J] {
  def jsonR(mode: AsyncParser.Mode): Channel[Task, Option[ByteVector], Seq[J]] = {
    val acquire = Task.delay(AsyncParser[J](mode))
    def flush(p: AsyncParser[J]) = Task.delay(p.finish().fold(throw _, identity))
    def step(p: AsyncParser[J]) = Task.now { bytes: ByteVector => Task.delay {
      p.absorb(bytes.toByteBuffer).fold(throw _, identity)
    }}
    io.bufferedChannel(acquire)(flush)(_ => Task.now(()))(step)
  }

  override def parseJson(p: Process[Task, ByteVector]): Task[J] =
    p.throughOption(jsonR(AsyncParser.SingleValue)).flatMap(emitAll).runLastOr(facade.jnull())

  override def parseJsonArray(p: Process[Task, ByteVector]): Process[Task, Seq[J]] =
    p.throughOption(jsonR(AsyncParser.UnwrapArray))

  override def parseJsonStream(p: Process[Task, ByteVector]): Process[Task, Seq[J]] =
    p.throughOption(jsonR(AsyncParser.ValueStream))
}

