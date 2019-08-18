package org.http4s
package blazecore
package websocket

import cats.effect._
import cats.effect.concurrent.Deferred
import cats.implicits._
import fs2._
import fs2.concurrent.SignallingRef
import java.util.concurrent.atomic.AtomicBoolean
import org.http4s.blaze.pipeline.{LeafBuilder, TailStage, TrunkBuilder}
import org.http4s.blaze.pipeline.Command.EOF
import org.http4s.blaze.util.Execution.{directec, trampoline}
import org.http4s.internal.unsafeRunAsync
import org.http4s.websocket.{WebSocket, WebSocketFrame}
import org.http4s.websocket.WebSocketFrame._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

private[http4s] class Http4sWSStage[F[_]](
    ws: WebSocket[F]
)(implicit F: ConcurrentEffect[F], val ec: ExecutionContext)
    extends TailStage[WebSocketFrame] {

  @deprecated("Preserved for binary compatibility", "0.20.11")
  def this(ws: WebSocket[F], sentClose: AtomicBoolean, deadSignal: SignallingRef[F, Boolean])(
      implicit F: ConcurrentEffect[F],
      ec: ExecutionContext) =
    this(ws)

  def name: String = "Http4s WebSocket Stage"

  private val sentClose = Deferred.unsafe[F, Unit]
  private val receivedClose = Deferred.unsafe[F, Unit]

  private def trySendClose(close: Close): F[Unit] =
    sentClose.complete(()).attempt.flatMap {
      case Right(()) => writeFrame(close, directec)
      case Left(_) => F.unit // already closed
    }

  //////////////////////// Source and Sink generators ////////////////////////
  def snk: Pipe[F, WebSocketFrame, Unit] = {
    def go(s: Stream[F, WebSocketFrame]): Pull[F, Unit, Unit] =
      s.pull.uncons1.flatMap {
        case Some((close: Close, _)) =>
          Pull.eval(trySendClose(close)) >> Pull.done
        case Some((frame, tail)) =>
          Pull.eval(writeFrame(frame, directec)) >> go(tail)
        case None =>
          Pull.eval(trySendClose(Close(1000, "").fold(throw _, identity))) >> Pull.done
      }
    go(_).stream
  }

  private[this] def writeFrame(frame: WebSocketFrame, ec: ExecutionContext): F[Unit] =
    F.async[Unit] { cb =>
      channelWrite(frame).onComplete {
        case Success(res) => cb(Right(res))
        case Failure(t) => cb(Left(t))
      }(ec)
    }

  private[this] def readFrameTrampoline: F[WebSocketFrame] = F.async[WebSocketFrame] { cb =>
    channelRead().onComplete {
      case Success(ws) => cb(Right(ws))
      case Failure(exception) => cb(Left(exception))
    }(trampoline)
  }

  private[this] val handleRead: F[WebSocketFrame] =
    readFrameTrampoline.flatMap {
      case close: Close =>
        // Acknowledge that we received the closing handshake
        receivedClose.complete(()).attempt >>
          // If we haven't sent a close, reply with it
          trySendClose(close)
          // And pass it along
            .as(close)
      case Ping(d) =>
        // Reply to ping frame immediately
        writeFrame(Pong(d), trampoline) >> handleRead
      case _: Pong =>
        // Don't forward pong frame
        handleRead
      case rest =>
        F.pure(rest)
    }

  def inputstream: Stream[F, WebSocketFrame] =
    Stream.repeatEval(handleRead).takeThrough {
      case _: Close => false
      case _ => true
    }

  //////////////////////// Startup and Shutdown ////////////////////////

  override protected def stageStartup(): Unit = {
    super.stageStartup()

    val closingHandshake = Stream.eval(receivedClose.get) ++ Stream.eval(sentClose.get)

    val wsStream = closingHandshake
      .concurrently(inputstream.through(ws.receive))
      .concurrently(ws.send.through(snk))
      .onFinalize(ws.onClose.attempt.void)
      .compile
      .drain

    unsafeRunAsync(wsStream) {
      case Left(EOF) =>
        IO(stageShutdown())
      case Left(t) =>
        IO(logger.error(t)("Error closing Web Socket"))
      case Right(_) =>
        // Nothing to do here
        IO.unit
    }
  }
}

object Http4sWSStage {
  def bufferingSegment[F[_]](stage: Http4sWSStage[F]): LeafBuilder[WebSocketFrame] =
    TrunkBuilder(new SerializingStage[WebSocketFrame]).cap(stage)
}
