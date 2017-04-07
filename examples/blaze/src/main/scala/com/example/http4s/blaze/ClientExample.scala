package com.example.http4s.blaze

object ClientExample {

  def getSite() = {

    import org.http4s.Http4s._
    import cats.effect.IO

    val client = org.http4s.client.blaze.SimpleHttp1Client()

    val page: IO[String] = client.expect[String](uri("https://www.google.com/"))

    for (_ <- 1 to 2)
      println(page.map(_.take(72)).unsafeRunSync())   // each execution of the IO will refetch the page!

    // We can do much more: how about decoding some JSON to a scala object
    // after matching based on the response status code?
    import org.http4s.Status.{NotFound, Successful}
    import io.circe._
    import io.circe.generic.auto._
    import org.http4s.circe.jsonOf

    final case class Foo(bar: String)

    // jsonOf is defined for Json4s, Argonuat, and Circe, just need the right decoder!
    implicit val fooDecoder = jsonOf[Foo]

    // Match on response code!
    val page2 = client.get(uri("http://http4s.org/resources/foo.json")) {
      case Successful(resp) => resp.as[Foo].map("Received response: " + _)
      case NotFound(resp)   => IO.now("Not Found!!!")
      case resp             => IO.now("Failed: " + resp.status)
    }

    println(page2.unsafeRunSync())

    client.shutdownNow()
  }

  def main(args: Array[String]): Unit = getSite()

}
