package org.http4s
package blaze.client

import Method._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scalaz.concurrent.Task

import org.http4s.client.ClientSyntax

class BlazeHttp1ClientSpec extends WordSpec with Matchers {

  def gatherBody(body: HttpBody): String = {
    new String(body.runLog.run.map(_.toArray).flatten.toArray)
  }

  "Blaze Simple Http1 Client" should {
    implicit def client = SimpleHttp1Client

    "Make simple http requests" in {
      val resp = Get("http://www.google.com/").exec.run
      val thebody = gatherBody(resp.body)
//      println(resp.copy(body = halt))

      resp.status.code should equal(200)
    }

    "Make simple https requests" in {
      val resp = Get("https://www.google.com/").exec.run
      val thebody = gatherBody(resp.body)
//      println(resp.copy(body = halt))
//      println("Body -------------------------\n" + gatherBody(resp.body) + "\n--------------------------")
      resp.status.code should equal(200)
    }
  }

  "RecyclingHttp1Client" should {
    implicit val client = new PooledHttp1Client()

    "Make simple http requests" in {
      val resp = Get("http://www.google.com/").exec.run
      val thebody = gatherBody(resp.body)
      //      println(resp.copy(body = halt))

      resp.status.code should equal(200)
    }

    "Repeat a simple http request" in {
      val f = 0 until 10 map { _ =>
        Future {
          val resp = Get("http://www.google.com/").exec.run
          val thebody = gatherBody(resp.body)
          //      println(resp.copy(body = halt))

          resp.status.code should equal(200)
        }
      } reduce((f1, f2) => f1.flatMap(_ => f2))

      Await.result(f, 10.seconds)
    }

    "Make simple https requests" in {
      val resp = Get("https://www.google.com/").exec.run
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
