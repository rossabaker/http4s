package org.http4s

import cats.data._
import cats.effect.IO
import org.http4s.batteries._

object DecodeResult {
  def apply[A](fa: IO[Either[DecodeFailure, A]]): DecodeResult[A] =
    EitherT(fa)

  def success[A](a: IO[A]): DecodeResult[A] =
    DecodeResult(a.map(right(_)))

  def success[A](a: A): DecodeResult[A] =
    success(IO.now(a))

  def failure[A](e: IO[DecodeFailure]): DecodeResult[A] =
    DecodeResult(e.map(left(_)))

  def failure[A](e: DecodeFailure): DecodeResult[A] =
    failure(IO.now(e))
}
