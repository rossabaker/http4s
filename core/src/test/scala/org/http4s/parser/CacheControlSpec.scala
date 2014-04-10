package org.http4s.parser

import org.scalatest.{Matchers, WordSpec}
import org.http4s.Header.`Cache-Control`
import scalaz.Validation
import org.http4s.CacheDirective, CacheDirective._
import scala.concurrent.duration._
import org.http4s.util.string._

/**
 * @author Bryce Anderson
 *         Created on 1/29/14
 */
class CacheControlSpec extends WordSpec with Matchers with HeaderParserHelper[`Cache-Control`] {
  def hparse(value: String): Validation[ParseErrorInfo, `Cache-Control`] = HttpParser.CACHE_CONTROL(value)

  // Default values
  val valueless = List(`no-store`, `no-transform`, `only-if-cached`,
                       `public`, `must-revalidate`, `proxy-revalidate`)

  val numberdirectives = List(`max-age`(0.seconds), `min-fresh`(1.second), `s-maxage`(2.seconds),
                              `stale-if-error`(3.seconds), `stale-while-revalidate`(4.seconds))

  val strdirectives = List(`private`("Foo".ci::Nil), `private`(Nil), `no-cache`("Foo".ci::Nil), `no-cache`())

  val others = List(`max-stale`(None),
                    `max-stale`(Some(2.seconds)),
                    CacheDirective.fromString("Foo"),
                    CacheDirective.fromString("Foo=Bar"))


  "CacheControl parser" should {

    "Generate correct directive values" in {
      valueless.foreach { v =>
        v.value should equal(v.name.toString)
      }

      numberdirectives.zipWithIndex.foreach{ case (v, i) =>
        v.value should equal(s"${v.name}=$i")
      }

      `max-stale`(None).value should equal("max-stale")
      `max-stale`(Some(2.seconds)).value should equal("max-stale=2")

      CacheDirective.fromString("Foo=Bar").value should equal("Foo=\"Bar\"")
      CacheDirective.fromString("Foo").value should equal("Foo")
    }

    "Parse cache headers" in {
      val all = valueless ::: numberdirectives ::: strdirectives ::: others

      all.foreach { d =>
        val h = `Cache-Control`(d)
        parse(h.value) should equal(h)
      }
    }
  }
}
