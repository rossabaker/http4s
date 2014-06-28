package org.http4s.blaze
package client

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import org.http4s.blaze.pipeline.{HeadStage, Command}
import org.http4s.{Response, Request}
import org.http4s.client.Client

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scalaz.{\/-, -\/}

import scalaz.concurrent.Task
import scalaz.stream.Process.eval_

/**
 * Created by Bryce Anderson on 6/24/14.
 */

/** Base on which to implement a BlazeClient */
trait BlazeClient extends PipelineBuilder with Client {

  implicit protected def ec: ExecutionContext

  override def request(req: Request): Task[Response] = Task.async { cb =>
    val PipelineResult(builder, tail, addr) = buildPipeline(req, true)

    getConnection(addr).onComplete {
      case Success(head) =>
        builder.base(head)
        head.sendInboundCommand(Command.Connected)
        tail.runRequest(req).runAsync {
          case \/-(r)    =>
            val endgame = eval_(Task.delay {
              if (!tail.isClosed()) {
                recycleConnection(addr, tail)
              }
            })

            cb(\/-(r.copy(body = r.body.onComplete(endgame))))

          case e@ -\/(_) =>
            if (!tail.isClosed()) {
              tail.sendOutboundCommand(Command.Disconnect)
            }
            cb(e)
        }

      case Failure(t) => cb (-\/(t))
    }
  }

  /** Recycle or close the connection
    * Allow for smart reuse or simple closing of a connection after the completion of a request
    * @param addr InetSocketAddress of the connection
    * @param stage the [[BlazeClientStage]] which to deal with
    */
  protected def recycleConnection(addr: InetSocketAddress, stage: BlazeClientStage): Unit

  /** Get a connection to the provided address
    * @param addr InetSocketAddress to connect too
    * @return a Future with the connected [[HeadStage]] of a blaze pipeline
    */
  protected def getConnection(addr: InetSocketAddress): Future[HeadStage[ByteBuffer]]
}