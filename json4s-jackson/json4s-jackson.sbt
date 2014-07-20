import Http4sDependencies._

name := "http4s-json4s-jackson"

description := "json4s I/O support for http4s, using Jackson backend"

libraryDependencies ++= Seq(
  json4sJackson
)

