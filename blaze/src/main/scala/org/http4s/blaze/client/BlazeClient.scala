package org.http4s.blaze
package client

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import org.http4s.blaze.pipeline.{HeadStage, Command, LeafBuilder}
import org.http4s.blaze.pipeline.stages.SSLStage
import org.http4s.blaze.util.{Execution}
import org.http4s.{Response, Request}
import org.http4s.client.Client
import org.http4s.blaze.channel.nio2.ClientChannelFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scalaz.-\/
import scalaz.concurrent.Task

/**
 * Created by Bryce Anderson on 6/24/14.
 */

/** Base on which to implement a BlazeClient */
trait BlazeClient extends PipelineBuilder with Client {

  implicit protected def ec: ExecutionContext

  protected def connectionManager: ClientChannelFactory

  protected def bufferSize: Int = 8*1024


  /** Shutdown this client, closing any open connections and freeing resources */
  override def shutdown(): Task[Unit] = Task.now(())

  override def request(req: Request): Task[Response] = makeRequest(req)

  protected def getConnection(addr: InetSocketAddress, close: Boolean): Future[HeadStage[ByteBuffer]] = {
    connectionManager.connect(addr, bufferSize)
  }

  private def makeRequest(req: Request): Task[Response] = Task.async { cb =>
    val PipelineResult(builder, tail, addr) = buildPipeline(req, true)
    getConnection(addr, true).onComplete {
      case Success(head) =>
        builder.base(head)
        head.sendInboundCommand(Command.Connected)
        tail.runRequest(req).runAsync(cb)

      case Failure(t)    => cb (-\/(t))
    }
  }

//  protected def buildPipeline(req: Request, closeOnFinish: Boolean): PipelineResult = {
//
//    val isHttps = req.requestUri.scheme.map { ci =>
//      if (ci == "https") true
//      else false
//    }.getOrElse(false)
//

//
//    fhead.map { head =>
//      val t = new BlazeClientStage(closeOnFinish)
//
//      if (isHttps) {
//        val eng = sslContext.createSSLEngine()
//        eng.setUseClientMode(true)
//        LeafBuilder(t).prepend(new SSLStage(eng)).base(head)
//      } else LeafBuilder(t).base(head)
//
//      head.sendInboundCommand(Command.Connected)
//      t
//    }
//  }
}

/** A default implementation of the Blaze Asynchronous client */
object BlazeClient extends BlazeClient with Http1Support with Http1SSLSupport {
  override implicit protected def ec: ExecutionContext = Execution.trampoline

  override protected def connectionManager: ClientChannelFactory = new ClientChannelFactory()
}
