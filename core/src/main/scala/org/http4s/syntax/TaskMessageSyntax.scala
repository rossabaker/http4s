package org.http4s
package syntax

import cats._
import cats.data.EitherT
import cats.implicits._

trait TaskMessageOps[F[+_], M <: Message[F]] extends Any with MessageOps[F] {
  type Self = F[M#Self]

  def self: F[M]

  def transformHeaders(f: Headers => Headers)(implicit F: Functor[F]): Self =
    self.map(_.transformHeaders(f))

  def withBody[T](b: T)(implicit w: EntityEncoder[F, T], F: FlatMap[F]): Self =
    self.flatMap(_.withBody(b))

  override def withAttribute[A](key: AttributeKey[A], value: A)(implicit F: FlatMap[F]): Self =
    self.map(_.withAttribute(key, value))

  override def attemptAs[T](implicit decoder: EntityDecoder[F, T], F: FlatMap[F]): DecodeResult[F, T] = EitherT(self.flatMap { msg =>
    decoder.decode(msg, false).value
  })
}
