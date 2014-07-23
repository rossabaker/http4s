package org.http4s.blaze.client

import java.nio.ByteBuffer

import org.http4s.{Response, Request}
import org.http4s.blaze.pipeline.TailStage

import scalaz.concurrent.Task

trait BlazeClientStage extends TailStage[ByteBuffer] {
  def runRequest(req: Request): Task[Response]

  def isClosed(): Boolean

  def shutdown(): Unit
}
