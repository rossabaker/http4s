package org.http4s
package server
package middleware

import cats.{Functor, Monad, MonoidK}
import cats.data.Kleisli
import cats.implicits._

/** Removes a trailing slash from [[Request]] path
  *
  * If a route exists with a file style [[Uri]], eg "/foo",
  * this middleware will cause [[Request]]s with uri = "/foo" and
  * uri = "/foo/" to match the route.
  */
object AutoSlash {
  def apply[F[_]: Monad, G[_]: Functor, A](service: Kleisli[F, Request[G], A])(implicit F: MonoidK[F]): Kleisli[F, Request[G], A] =
    service <+> Kleisli { req =>
      val pi = req.pathInfo
      if (pi.isEmpty || pi.charAt(pi.length - 1) != '/')
        F.empty
      else
        service.apply(req.withPathInfo(pi.substring(0, pi.length - 1)))
    }
}
