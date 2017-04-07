package org.http4s

import cats.data._
import cats.effect._
import fs2._

case class AuthedRequest[A](authInfo: A, req: Request)

object AuthedRequest {
  def apply[T](getUser: Request => IO[T]): Kleisli[IO, Request, AuthedRequest[T]] = Kleisli({ request =>
    getUser(request).map(user => AuthedRequest(user, request))
  })
}
