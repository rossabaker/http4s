package org.http4s

import cats.Applicative
import cats.data.Kleisli

object Http {
  def fromPartial[F[_]](pf: PartialFunction[Request[F], F[Response[F]]], notFound: Response[F] = Response.notFound[F])(implicit F: Applicative[F]): Http[F] =
    Kleisli(pf.lift.andThen(_.getOrElse(F.pure(notFound))))
}
