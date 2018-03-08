package org.http4s

import cats.Applicative
import cats.data.{Kleisli, OptionT}
import cats.implicits._
import cats.effect.Sync

object HttpPartial {
  def apply[F[_]](pf: PartialFunction[Request[F], F[Response[F]]])(implicit F: Sync[F]): HttpPartial[F] =
    Kleisli { req => OptionT(F.delay(pf.lift(req).sequence).flatten) }

  def empty[F[_]: Applicative]: HttpPartial[F] = Kleisli.liftF(OptionT.none)
}
