package org.http4s.blaze
package client

import java.net.InetSocketAddress
import java.security.KeyStore
import javax.net.ssl.{SSLContext, KeyManagerFactory}

import org.http4s.blaze.pipeline.{Command, LeafBuilder}
import org.http4s.blaze.pipeline.stages.SSLStage
import org.http4s.blaze.pipeline.stages.http.HttpClientStage
import org.http4s.blaze.util.BogusKeystore
import org.http4s.{Response, Request}
import org.http4s.client.Client
import org.http4s.blaze.channel.nio2.ClientChannelFactory


import scala.concurrent.{ExecutionContext, Future}
import scalaz.concurrent.Task

/**
 * Created by Bryce Anderson on 6/24/14.
 */
abstract class BlazeClient extends Client {

  implicit protected def ec: ExecutionContext

  protected def connectionManager: ClientChannelFactory

  protected def bufferSize: Int = 8*1024

  override def request(urls: Seq[Request]): Task[Seq[Response]] = {
    ???
  }

  private lazy val sslContext = {
    val ksStream = BogusKeystore.asInputStream()
    val ks = KeyStore.getInstance("JKS")
    ks.load(ksStream, BogusKeystore.getKeyStorePassword)

    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    kmf.init(ks, BogusKeystore.getCertificatePassword)

    val context = SSLContext.getInstance("SSL")

    context.init(kmf.getKeyManagers(), null, null)
    context
  }

  private def makeRequest(req: Request, client: HttpClientStage): Future[_] = {
    val f = client.makeRequest(req.requestMethod.name,
      req.requestUri.host.get.toString,
      req.requestUri.path,
      req.headers.map(h => (h.name.toString, h.value)),
      body = ???)
    // Shutdown our connection
    f.onComplete( _ => client.sendOutboundCommand(Command.Disconnect))
    f
  }

  private def buildPipeline(req: Request, closeOnFinish: Boolean): Future[BlazeClientStage] = {

    val isHttps = req.requestUri.scheme.map { ci =>
      if (ci == "https") true
      else false
    }.getOrElse(false)

    val auth = req.requestUri.authority.get
    val port = auth.port.getOrElse(if (isHttps) 443 else 80)
    val fhead = connectionManager.connect(new InetSocketAddress(auth.host.toString, port), bufferSize)

    fhead.map { head =>
      val t = new BlazeClientStage(closeOnFinish)

      if (isHttps) {
        val eng = sslContext.createSSLEngine()
        eng.setUseClientMode(true)
        LeafBuilder(t).prepend(new SSLStage(eng)).base(head)
      }
      else LeafBuilder(t).base(head)
      head.sendInboundCommand(Command.Connected)
      t
    }
  }
}
