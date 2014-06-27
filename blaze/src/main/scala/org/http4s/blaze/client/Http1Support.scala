package org.http4s.blaze.client

import java.net.InetSocketAddress

import org.http4s.Request
import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.util.CaseInsensitiveString._

import scala.concurrent.ExecutionContext

/**
 * Created by Bryce Anderson on 6/26/14.
 */
trait Http1Support extends PipelineBuilder {

  implicit protected def ec: ExecutionContext

  override protected def buildPipeline(req: Request, closeOnFinish: Boolean): PipelineResult = {
    val isHttp = req.requestUri.scheme match {
      case Some(s) if s != "http".ci => false
      case _ => true
    }

    if (isHttp && req.requestUri.authority.isDefined) {
      val auth = req.requestUri.authority.get
      val t = new BlazeClientStage(closeOnFinish)
      val b = LeafBuilder(t)
      val port = auth.port.getOrElse(80)
      val address = new InetSocketAddress(auth.host.toString, port)
      PipelineResult(b, t, address)
    }
    else super.buildPipeline(req, closeOnFinish)
  }
}
