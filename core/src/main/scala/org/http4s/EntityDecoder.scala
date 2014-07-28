package org.http4s

import scala.util.control.NonFatal
import scalaz.concurrent.Task
import scalaz.stream.processes

sealed trait EntityDecoder[+T] { self =>

  final def apply(msg: Message): Task[T] = decode(msg)

  def decode(msg: Message): Task[T]

  def consumes: Set[MediaRange]

  def map[T2](f: T => T2): EntityDecoder[T2] = new EntityDecoder[T2] {
    override def consumes: Set[MediaRange] = self.consumes

    override def decode(msg: Message): Task[T2] = self.decode(msg).map(f)
  }

  def orElse[T2 >: T](other: EntityDecoder[T2]): EntityDecoder[T2] = new EntityDecoder.OrDec(this, other)

  def matchesMediaType(msg: Message): Boolean = {
    if (!consumes.isEmpty) {
      msg.headers.get(Header.`Content-Type`).flatMap {
        h => consumes.find { m => m == h.mediaType.satisfiedBy(m) }
      }.isDefined
    }
    else false
  }
}

object EntityDecoder {
  def apply[T](f: Message => Task[T], valid: MediaRange*): EntityDecoder[T] = new EntityDecoder[T] {
    override def decode(msg: Message): Task[T] = {
      try f(msg)
      catch { case NonFatal(e) => Task.fail(e) }
    }

    override val consumes: Set[MediaRange] = valid.toSet
  }
  
  private class OrDec[+T](a: EntityDecoder[T], b: EntityDecoder[T]) extends EntityDecoder[T] {
    override def decode(msg: Message): Task[T] = {
      if (a.matchesMediaType(msg)) a.decode(msg)
      else b.decode(msg)
    }

    override val consumes: Set[MediaRange] = a.consumes ++ b.consumes
  }

  /////////////////// Instances //////////////////////////////////////////////
  implicit lazy val text: EntityDecoder[String] = {
    def decodeString(msg: Message): Task[String] = {
      val buff = new StringBuilder
      (msg.body |> processes.fold(buff) { (b, c) => {
        b.append(new String(c.toArray, (msg.charset.charset)))
      }}).map(_.result()).runLastOr("")
    }
    EntityDecoder(decodeString, MediaType.`text/plain`)
  }

}