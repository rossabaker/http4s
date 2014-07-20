import Http4sDependencies._

name := "http4s-json4s-native"

description := "json4s I/O support for http4s, using native backend"

libraryDependencies ++= Seq(
  json4sNative
)
