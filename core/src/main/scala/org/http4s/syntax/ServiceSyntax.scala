package org.http4s
package syntax

import cats.effect.IO

trait ServiceSyntax {
  implicit def http4sServiceSyntax[A, B](service: Service[A, B]): ServiceOps[A, B] =
    new ServiceOps[A, B](service)
}

final class ServiceOps[A, B](self: Service[A, B]) {
  def orNotFound(a: A)(implicit ev: B <:< MaybeResponse): IO[Response] =
    self.run(a).map(_.orNotFound)
}
