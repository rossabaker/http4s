package com.github.http4s.test

case class SimpleResponse(
  status: Int,
  headers: Map[String, Seq[String]],
  body: String
)
