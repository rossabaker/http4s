package org.http4s

import cats.effect.IO

package object servlet {
  protected[servlet] type BodyWriter = Response => IO[Unit]

  protected[servlet] val NullBodyWriter: BodyWriter = { _ => IO.now(()) }

  protected[servlet] val DefaultChunkSize = 4096
}
