package org.http4s.websocket

import cats.effect.IO
import fs2._

import org.http4s.websocket.WebsocketBits.WebSocketFrame

private[http4s] final case class Websocket(
  read: Stream[IO, WebSocketFrame],
  write: Sink[IO, WebSocketFrame]
)

