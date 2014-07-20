package org.http4s.json

import org.http4s.Header.`Content-Type`
import org.http4s.{HttpBody, Writable}

trait JsonWriter[J] {
  implicit def jsonWritable: Writable[J]
}

trait JsonWritable[J] extends Writable[J] {
  override def contentType: `Content-Type` = `Content-Type`.`application/json`
}
