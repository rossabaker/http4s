package org.http4s
package client
package blaze

import cats.effect.IO
import fs2._
import org.http4s.blaze.pipeline.Command
import org.log4s.getLogger

/** Blaze client implementation */
object BlazeClient {
  private[this] val logger = getLogger

  /** Construct a new [[Client]] using blaze components
    *
    * @param manager source for acquiring and releasing connections. Not owned by the returned client.
    * @param config blaze client configuration.
    * @param onShutdown arbitrary tasks that will be executed when this client is shutdown
    */
  def apply[A <: BlazeConnection](manager: ConnectionManager[A],
                                  config: BlazeClientConfig,
                                  onShutdown: IO[Unit]): Client = {

    Client(Service.lift { req =>
      val key = RequestKey.fromRequest(req)

      // If we can't invalidate a connection, it shouldn't tank the subsequent operation,
      // but it should be noisy.
      def invalidate(connection: A): IO[Unit] =
        manager.invalidate(connection).handle {
          case e => logger.error(e)("Error invalidating connection")
        }

      def loop(next: manager.NextConnection): IO[DisposableResponse] = {
        // Add the timeout stage to the pipeline
        val ts = new ClientTimeoutStage(config.idleTimeout, config.requestTimeout, bits.ClientTickWheel)
        next.connection.spliceBefore(ts)
        ts.initialize()

        next.connection.runRequest(req).attempt.flatMap {
          case Right(r)  =>
            val dispose = IO.delay(ts.removeStage)
              .flatMap { _ => manager.release(next.connection) }
            IO.now(DisposableResponse(r, dispose))

          case Left(Command.EOF) =>
            invalidate(next.connection).flatMap { _ =>
              if (next.fresh) IO.fail(new java.io.IOException(s"Failed to connect to endpoint: $key"))
              else {
                manager.borrow(key).flatMap { newConn =>
                  loop(newConn)
                }
              }
            }

          case Left(e) =>
            invalidate(next.connection).flatMap { _ =>
              IO.fail(e)
            }
        }
      }
      manager.borrow(key).flatMap(loop)
    }, onShutdown)
  }
}

