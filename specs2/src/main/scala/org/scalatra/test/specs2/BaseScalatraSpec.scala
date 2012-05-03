package com.github.http4s
package test
package specs2

import org.eclipse.jetty.testing.ServletTester
import org.specs2.specification.{BaseSpecification, Step, Fragments}

/**
 * A base specification structure that starts the tester before the
 * specification and stops it afterward.  Clients probably want to extend
 * Http4sSpec or MutableHttp4sSpec.
 */
trait BaseHttp4sSpec extends BaseSpecification with Http4sTests {
  lazy val tester = new ServletTester

  override def map(fs: =>Fragments) = Step(start()) ^ super.map(fs) ^ Step(stop())
}
