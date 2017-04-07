package org.http4s
package client

import cats.effect.IO

private final class BasicManager[A <: Connection](builder: ConnectionBuilder[A]) extends ConnectionManager[A] {
  def borrow(requestKey: RequestKey): IO[NextConnection] =
    builder(requestKey).map(NextConnection(_, true))

  override def shutdown(): IO[Unit] =
    IO.now(())

  override def invalidate(connection: A): IO[Unit] =
    IO.delay(connection.shutdown())

  override def release(connection: A): IO[Unit] =
    invalidate(connection)
}
