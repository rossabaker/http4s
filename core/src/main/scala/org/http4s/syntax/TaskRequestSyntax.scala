package org.http4s
package syntax

import cats.effect.IO

trait TaskRequestSyntax {
  implicit def http4sTaskRequestSyntax(req: IO[Request]): TaskRequestOps =
    new TaskRequestOps(req)
}

final class TaskRequestOps(val self: IO[Request])
    extends AnyVal
    with TaskMessageOps[Request]
    with RequestOps {
  def decodeWith[A](decoder: EntityDecoder[A], strict: Boolean)(f: A => IO[Response]): IO[Response] =
    self.flatMap(_.decodeWith(decoder, strict)(f))

  def withPathInfo(pi: String): IO[Request] =
    self.map(_.withPathInfo(pi))
}
