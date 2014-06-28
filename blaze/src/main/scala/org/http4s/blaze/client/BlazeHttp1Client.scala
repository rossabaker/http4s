package org.http4s.blaze.client

import org.http4s.blaze.channel.nio2.ClientChannelFactory
import org.http4s.blaze.pipeline.{Command, HeadStage}
import org.http4s.blaze.util.Execution

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import scala.concurrent.{Future, ExecutionContext}

import scalaz.concurrent.Task

/**
 * Created by Bryce Anderson on 6/27/14.
 */

/** A default implementation of the Blaze Asynchronous client for HTTP/1.x */
abstract class BlazeHttp1Client(bufferSize: Int = 8*1024)
       extends BlazeClient
          with Http1Support
          with Http1SSLSupport
{
  override implicit protected def ec: ExecutionContext = Execution.trampoline

    /** Shutdown this client, closing any open connections and freeing resources */
  override def shutdown(): Task[Unit] = Task.now(())

  protected val connectionManager = new ClientChannelFactory()

  override protected def getConnection(addr: InetSocketAddress): Future[HeadStage[ByteBuffer]] = {
    connectionManager.connect(addr, bufferSize)
  }

  override protected def recycleConnection(addr: InetSocketAddress, stage: BlazeClientStage): Unit = {
    if (!stage.isClosed()) stage.sendOutboundCommand(Command.Disconnect)
  }
}

object BlazeHttp1Client extends BlazeHttp1Client(8*1024)