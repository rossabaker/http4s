import sbt._
import Keys._

object Http4sBuild extends Build {
  lazy val http4sSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.github.http4s",
    version := "0.0.1-SNAPSHOT",
    // Why 2.9.0-1?  Because Scala is forward compatible.
    scalaVersion := "2.9.2",
    crossPaths := false
  )

  val commonsLang3 = "org.apache.commons" % "commons-lang3" % "3.1"
  val dispatch = "net.databinder" %% "dispatch-http" % "0.8.8"
  val grizzledSlf4j = "org.clapper" %% "grizzled-slf4j" % "0.6.10"
  val junit = "junit" % "junit" % "4.10"
  val mockitoAll = "org.mockito" % "mockito-all" % "1.8.5"
  val scalatest = "org.scalatest" %% "scalatest" % "1.6.1"
  //val specs = "org.scala-tools.testing" %% "specs" % "1.6.8"
  val specs2 = "org.specs2" %% "specs2" % "1.12.3"
  val servletApi = "javax.servlet" % "javax.servlet-api" % "3.0.1"
  val testng = "org.testng" % "testng" % "6.3" % "optional"

  lazy val http4s = Project(
    id = "http4s",
    base = file("."),
    settings = http4sSettings
  ) // aggregate (http4sScalatest, http4sSpecs2)

  lazy val http4sTest = Project(
    id = "http4s-test",
    base = file("test"),
    settings = http4sSettings ++ Seq(
      libraryDependencies ++= Seq(
	dispatch, 
	grizzledSlf4j,
	servletApi, 
	specs2 % "test"
      ),
      ivyXML := <dependencies>
        <dependency org="org.eclipse.jetty" name="test-jetty-servlet" rev="8.1.3.v20120416">
          <exclude org="org.eclipse.jetty.orbit" />
        </dependency>
      </dependencies>
    )
  )

  lazy val http4sScalatest = Project(
    id = "http4s-scalatest",
    base = file("scalatest"),
    settings = http4sSettings ++ Seq(
      libraryDependencies ++= Seq(scalatest, junit, testng)
    )
  ) dependsOn(http4sTest % "compile;test->test;provided->provided")

/*
  lazy val http4sSpecs = Project(
    id = "http4s-specs",
    base = file("specs"),
    settings = http4sSettings ++ Seq(
      libraryDependencies += specs
    )
  ) dependsOn(http4sTest % "compile;test->test;provided->provided")
  */

  lazy val http4sSpecs2 = Project(
    id = "http4s-specs2",
    base = file("specs2"),
    settings = http4sSettings ++ Seq(
      libraryDependencies += specs2
    )
  ) dependsOn(http4sTest % "compile;test->test;provided->provided")

  object Dependencies {
  }
}
