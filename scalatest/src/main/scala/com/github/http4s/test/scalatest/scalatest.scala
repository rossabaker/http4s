package com.github.http4s.test
package scalatest

import org.junit.runner.RunWith
import org.eclipse.jetty.testing.ServletTester
import org.scalatest._
import org.scalatest.matchers.{MustMatchers, ShouldMatchers}
import org.scalatest.junit.{JUnitSuite, JUnit3Suite, JUnitRunner}
import org.scalatest.testng.TestNGSuite

@RunWith(classOf[JUnitRunner])
/**
 * Provides Http4s test support to ScalaTest suites.  The servlet tester
 * is started before the first test in the suite and stopped after the last.
 */
trait Http4sSuite extends Suite with Http4sTests with BeforeAndAfterAll with MustMatchers with ShouldMatchers {
  lazy val tester = new ServletTester

  override protected def beforeAll(): Unit = start()
  override protected def afterAll(): Unit = stop()
}

/**
 * Convenience trait to add Http4s test support to JUnit3Suite.
 */
trait Http4sJUnit3Suite extends JUnit3Suite with Http4sSuite

/**
 * Convenience trait to add Http4s test support to JUnitSuite.
 */
trait Http4sJUnitSuite extends JUnitSuite with Http4sSuite

/**
 * Convenience trait to add Http4s test support to TestNGSuite.
 */
trait Http4sTestNGSuite extends TestNGSuite with Http4sSuite

/**
 * Convenience trait to add Http4s test support to FeatureSpec.
 */
trait Http4sFeatureSpec extends FeatureSpec with Http4sSuite

/**
 * Convenience trait to add Http4s test support to Spec.
 */
trait Http4sSpec extends Spec with Http4sSuite

/**
 * Convenience trait to add Http4s test support to FlatSpec.
 */
trait Http4sFlatSpec extends FlatSpec with Http4sSuite

/**
 * Convenience trait to add Http4s test support to FreeSpec.
 */
trait Http4sFreeSpec extends FreeSpec with Http4sSuite

/**
 * Convenience trait to add Http4s test support to WordSpec.
 */
trait Http4sWordSpec extends WordSpec with Http4sSuite

/**
 * Convenience trait to add Http4s test support to FunSuite.
 */
trait Http4sFunSuite extends FunSuite with Http4sSuite
