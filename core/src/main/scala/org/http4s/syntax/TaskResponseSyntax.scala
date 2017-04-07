package org.http4s
package syntax

import cats.effect.IO

trait TaskResponseSyntax {
  implicit def http4sTaskResponseSyntax(resp: IO[Response]): TaskResponseOps =
    new TaskResponseOps(resp)
}

final class TaskResponseOps(val self: IO[Response])
    extends AnyVal
    with TaskMessageOps[Response]
    with ResponseOps {
  override def withStatus(status: Status): Self = self.map(_.withStatus(status))
}
