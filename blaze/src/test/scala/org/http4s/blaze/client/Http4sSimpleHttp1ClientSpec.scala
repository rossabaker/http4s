package org.http4s.blaze.client

import org.http4s.{HttpBody, Uri, Request, Response}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scalaz.stream.Process.halt

class Http4sSimpleHttp1ClientSpec extends WordSpec with Matchers {

  def gatherBody(body: HttpBody): String = {
    new String(body.runLog.run.map(_.toArray).flatten.toArray)
  }

  "Simple Http1 Client" should {
    def makeRequest(req: Request, close: Boolean = true): Response = SimpleHttp1Client.request(req).run

    "Make simple http requests" in {
      val req = Request(requestUri = Uri.fromString("http://www.google.com/").get)
      val resp = makeRequest(req)
      val thebody = gatherBody(resp.body)
//      println(resp.copy(body = halt))

      resp.status.code should equal(200)
    }

    "Make simple https requests" in {
      val req = Request(requestUri = Uri.fromString("https://www.google.com/").get)
      val resp = makeRequest(req)
      val thebody = gatherBody(resp.body)
//      println(resp.copy(body = halt))
//      println("Body -------------------------\n" + gatherBody(resp.body) + "\n--------------------------")
      resp.status.code should equal(200)
    }
  }

  "RecyclingHttp1Client" should {
    val client = new PooledHttp1Client()

    def makeRequest(req: Request, close: Boolean = true): Response = client.request(req).run

    "Make simple http requests" in {
      val req = Request(requestUri = Uri.fromString("http://www.google.com/").get)
      val resp = makeRequest(req)
      val thebody = gatherBody(resp.body)
      //      println(resp.copy(body = halt))

      resp.status.code should equal(200)
    }

    "Repeat a simple http request" in {
      val f = 0 until 10 map { _ =>
        Future {
          val req = Request(requestUri = Uri.fromString("http://www.google.com/").get)
          val resp = makeRequest(req)
          val thebody = gatherBody(resp.body)
          //      println(resp.copy(body = halt))

          resp.status.code should equal(200)
        }
      } reduce((f1, f2) => f1.flatMap(_ => f2))

      Await.result(f, 10.seconds)
    }

    "Make simple https requests" in {
      val req = Request(requestUri = Uri.fromString("https://www.google.com/").get)
      val resp = makeRequest(req)
      val thebody = gatherBody(resp.body)
      //      println(resp.copy(body = halt))
      //      println("Body -------------------------\n" + gatherBody(resp.body) + "\n--------------------------")
      resp.status.code should equal(200)
    }

    "Shutdown the client" in {
      client.shutdown().run
    }
  }
}
