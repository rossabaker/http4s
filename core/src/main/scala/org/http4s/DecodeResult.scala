package org.http4s

import cats._
import cats.data._
import fs2.Task
import org.http4s.batteries._

object DecodeResult {
  def apply[F[_], A](fa: F[Either[DecodeFailure, A]]): DecodeResult[F, A] =
    EitherT(fa)

  def success[F[_]: Functor, A](a: F[A]): DecodeResult[F, A] =
    DecodeResult(a.map(right(_)))

  def success[F[_]: Applicative, A](a: A): DecodeResult[F, A] =
    success(Applicative[F].pure(a))

  def failure[F[_]: Functor, A](e: F[DecodeFailure]): DecodeResult[F, A] =
    DecodeResult(e.map(left(_)))

  def failure[F[_]: Applicative, A](e: DecodeFailure): DecodeResult[F, A] =
    failure(Applicative[F].pure(e))
}
