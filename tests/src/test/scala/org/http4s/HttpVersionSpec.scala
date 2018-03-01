package org.http4s

import cats.implicits._
import cats.kernel.laws.discipline.OrderTests
import org.scalacheck.Prop._
import org.specs2.scalacheck.Parameters

class HttpVersionSpec extends Http4sSpec {
  import HttpVersion._

  checkAll("HttpVersion", OrderTests[HttpVersion].order)

  "sort by descending major version" in {
    prop { (x: HttpVersion, y: HttpVersion) =>
      x.major > y.major ==> (x > y)
    }
  }

  "sort by descending minor version if major versions equal" in {
    prop { (x: HttpVersion, y: HttpVersion) =>
      (x.major == y.major && x.minor > y.minor) ==> (x > y)
    }.setParameters(Parameters(maxDiscardRatio = 10.0f))
  }

  "fromString is consistent with toString" in {
    prop { v: HttpVersion =>
      fromString(v.toString) must beRight(v)
    }
  }

  "protocol is case sensitive" in {
    HttpVersion.fromString("http/1.0") must beLeft
  }
}
