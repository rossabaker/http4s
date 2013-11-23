package org.http4s.util.middleware

import org.http4s._
import scalaz.stream.Process._
import scala.util.control.ControlThrowable
import org.http4s.Status.RequestEntityTooLarge

object LimitEntitySize {
  val DefaultMaxEntitySize = Http4sConfig.getInt("org.http4s.default-max-entity-size")

  def constant(n: Int = DefaultMaxEntitySize)(service: HttpService): HttpService = { req =>
    val limitedBody = req.body |> limit(n)
    service(req.copy(body = limitedBody)).handle {
      case EntityTooLarge => RequestEntityTooLarge().run
    }
  }

  private case object EntityTooLarge extends ControlThrowable

  private def limit(n: Int): Process1[Chunk, Chunk] =
    if (n < 0)
      fail(EntityTooLarge)
    else
      await1[Chunk].flatMap { chunk => emit(chunk) fby limit(n - chunk.size) }
}
