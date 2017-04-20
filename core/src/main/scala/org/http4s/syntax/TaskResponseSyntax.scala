package org.http4s
package syntax

import cats._
import cats.implicits._
import fs2.Task

trait TaskResponseSyntax {
  implicit def http4sTaskResponseSyntax[F[_]](resp: F[Response[F]]): TaskResponseOps[F] =
    new TaskResponseOps[F](resp)
}

final class TaskResponseOps[F[_]](val self: F[Response[F]])
    extends AnyVal
    with TaskMessageOps[F, Response[F]]
    with ResponseOps[F] {
  override def withStatus(status: Status)(implicit F: Functor[F]): Self =
    self.map(_.withStatus(status))
}
