package org.http4s
package syntax

import cats.data.EitherT
import cats.effect.IO

trait TaskMessageOps[M <: Message] extends Any with MessageOps {
  type Self = IO[M#Self]

  def self: IO[M]

  def transformHeaders(f: Headers => Headers): Self =
    self.map(_.transformHeaders(f))

  def withBody[T](b: T)(implicit w: EntityEncoder[T]): Self = self.flatMap(_.withBody(b)(w))

  override def withAttribute[A](key: AttributeKey[A], value: A): Self = self.map(_.withAttribute(key, value))

  override def attemptAs[T](implicit decoder: EntityDecoder[T]): DecodeResult[T] = EitherT(self.flatMap { msg =>
    decoder.decode(msg, false).value
  })
}
