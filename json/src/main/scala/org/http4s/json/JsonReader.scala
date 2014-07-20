package org.http4s.json

import scodec.bits.ByteVector
import scalaz.concurrent.Task
import scalaz.stream.{Process}

trait JsonReader[J] {
  def parseJson(p: Process[Task, ByteVector]): Task[J]
  def parseJsonStream(p: Process[Task, ByteVector]): Process[Task, J]
  def parseJsonArray(p: Process[Task, ByteVector]): Process[Task, J]

  implicit class JsonSourceSyntax(self: Process[Task, ByteVector]) {
    def parseJson: Task[J] = JsonReader.this.parseJson(self)
    def parseJsonStream: Process[Task, J] = JsonReader.this.parseJsonStream(self)
    def parseJsonArray: Process[Task, J] = JsonReader.this.parseJsonArray(self)
  }
}


