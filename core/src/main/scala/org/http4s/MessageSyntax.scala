package org.http4s

import cats.effect.IO

object MessageSyntax extends MessageSyntax

trait MessageSyntax {
  @deprecated("Moved to org.http4s.syntax.TaskRequestSyntax", "0.16")
  implicit def requestSyntax(req: IO[Request]): syntax.TaskRequestOps =
    new syntax.TaskRequestOps(req)

  @deprecated("Moved to org.http4s.syntax.TaskResponseSyntax", "0.16")
  implicit def responseSyntax(resp: IO[Response]): syntax.TaskResponseOps =
    new syntax.TaskResponseOps(resp)
}
