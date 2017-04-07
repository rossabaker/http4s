package org.http4s
package client
package blaze

import java.nio.ByteBuffer

import cats.effect.IO
import org.http4s.blaze.pipeline.TailStage

private trait BlazeConnection extends TailStage[ByteBuffer] with Connection {
  def runRequest(req: Request): IO[Response]
}
