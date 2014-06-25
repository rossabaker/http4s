package org.http4s.blaze.client

import org.http4s.{HttpBody, Uri, Request, Response}
import org.scalatest.{Matchers, WordSpec}
import scalaz.stream.Process.halt

/**
 * Created by Bryce Anderson on 6/25/14.
 */
class Http4sHttp1ClientStageSpec extends WordSpec with Matchers {

  import org.http4s.Http4s._

  def makeRequest(req: Request, close: Boolean = true): Response = {
    BlazeClient.request(req).run
  }

  def gatherBody(body: HttpBody): String = {
    new String(body.runLog.run.map(_.toArray).flatten.toArray)
  }

  "Http1 Client" should {
    "Make simple requests" in {
      val req = Request(requestUri = Uri.fromString("http://www.google.com/").get)
      val resp = makeRequest(req)
      println(resp.copy(body = halt))
      println(gatherBody(resp.body))
      true should equal(true)
    }

  }

}
