package org.http4s

import cats._
import cats.data._

@deprecated("Renamed to HttpPartial", "0.19")
object HttpService extends Serializable {

  /**
    * Lifts a total function to an `HttpService`. The function is expected to
    * handle all requests it is given.  If `f` is a `PartialFunction`, use
    * `apply` instead.
    */
  @deprecated("Use liftF with an OptionT[F, Response[F]] instead", "0.18")
  def lift[F[_]: Functor](f: Request[F] => F[Response[F]]): HttpService[F] =
    Kleisli(f.andThen(OptionT.liftF(_)))

  /** Lifts a partial function to an `HttpService`.
    * Responds with `OptionT.none` for any request where `pf` is not defined.
    */
  @deprecated("Renamed to HttpPartial.apply, now requires Sync instance", "0.19")
  def apply[F[_]](pf: PartialFunction[Request[F], F[Response[F]]])(
      implicit F: Applicative[F]): HttpPartial[F] =
    Kleisli(req => pf.andThen(OptionT.liftF(_)).applyOrElse(req, Function.const(OptionT.none)))

  def empty[F[_]: Applicative]: HttpPartial[F] =
    HttpPartial.empty
}
