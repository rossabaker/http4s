package org.http4s.blaze.client

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import org.http4s.Request
import org.http4s.blaze.pipeline.LeafBuilder

/**
 * Created by Bryce Anderson on 6/26/14.
 */
trait PipelineBuilder {

  protected case class PipelineResult(builder: LeafBuilder[ByteBuffer],
                                      tail: BlazeClientStage,
                                      address: InetSocketAddress)

  protected def buildPipeline(req: Request, closeOnFinish: Boolean): PipelineResult = {
    sys.error(s"Unsupported request: ${req.requestUri}")
  }
}
