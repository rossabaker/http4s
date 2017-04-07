package org.http4s.client.blaze

import java.security.{NoSuchAlgorithmException, SecureRandom}
import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, X509TrustManager}
import java.util.concurrent._

import org.http4s.BuildInfo
import org.http4s.headers.{AgentProduct, `User-Agent`}
import org.http4s.blaze.util.TickWheelExecutor
import org.http4s.util.threads

import scala.concurrent.duration._
import cats.effect.IO

private[blaze] object bits {
  // Some default objects
  val DefaultTimeout: Duration = 60.seconds
  val DefaultBufferSize: Int = 8*1024
  val DefaultUserAgent = Some(`User-Agent`(AgentProduct("http4s-blaze", Some(BuildInfo.version))))

  val ClientTickWheel = new TickWheelExecutor()

  def getExecutor(config: BlazeClientConfig): (ExecutorService, IO[Unit]) = config.customExecutor match {
    case Some(exec) => (exec, IO.now(()))
    case None =>
      val exec = threads.newDaemonPool("http4s-blaze-client")
      (exec, IO.delay(exec.shutdown()))
  }

  /** Caution: trusts all certificates and disables endpoint identification */
  lazy val TrustingSslContext: SSLContext = {
    val trustManager = new X509TrustManager {
      def getAcceptedIssuers(): Array[X509Certificate] = Array.empty
      def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit = {}
      def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit = {}
    }
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, Array(trustManager), new SecureRandom)
    sslContext
  }
}
