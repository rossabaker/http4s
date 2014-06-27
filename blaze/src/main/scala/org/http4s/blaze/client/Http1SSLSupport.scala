package org.http4s
package blaze.client

import org.http4s.util.CaseInsensitiveString._

import java.net.InetSocketAddress
import java.security.KeyStore
import javax.net.ssl.{SSLContext, KeyManagerFactory}

import org.http4s.blaze.pipeline.LeafBuilder
import org.http4s.blaze.pipeline.stages.SSLStage
import org.http4s.blaze.util.BogusKeystore

import scala.concurrent.ExecutionContext


/**
 * Created by Bryce Anderson on 6/26/14.
 */
trait Http1SSLSupport extends Http1Support {

  implicit protected def ec: ExecutionContext

  private lazy val _clientSSLContext = {
    val ksStream = BogusKeystore.asInputStream()
    val ks = KeyStore.getInstance("JKS")
    ks.load(ksStream, BogusKeystore.getKeyStorePassword)

    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    kmf.init(ks, BogusKeystore.getCertificatePassword)

    val context = SSLContext.getInstance("SSL")

    context.init(kmf.getKeyManagers(), null, null)
    context
  }

  override protected def buildPipeline(req: Request, closeOnFinish: Boolean): PipelineResult = {
    req.requestUri.scheme match {
      case Some(ci) if ci == "https".ci && req.requestUri.authority.isDefined =>
        val eng = _clientSSLContext.createSSLEngine()
        eng.setUseClientMode(true)

        val auth = req.requestUri.authority.get
        val t = new BlazeClientStage(false)
        val b = LeafBuilder(t).prepend(new SSLStage(eng))
        val port = auth.port.getOrElse(443)
        val address = new InetSocketAddress(auth.host.toString, port)
        PipelineResult(b, t, address)

      case _ => super.buildPipeline(req, closeOnFinish)
    }
  }

}
