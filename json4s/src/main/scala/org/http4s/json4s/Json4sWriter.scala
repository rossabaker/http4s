package org.http4s.json4s

import org.http4s.{Writable, HttpBody}
import org.http4s.json.{JsonWritable, JsonWriter}
import org.json4s.JValue
import scalaz.concurrent.Task

trait Json4sWriter extends JsonWriter[JValue] {
  implicit override def jsonWritable: Writable[JValue] = new JsonWritable[JValue] {
    override def toBody(json: JValue): Task[(HttpBody, Option[Int])] = {
      implicitly[Writable[String]].toBody(render(json))
    }
  }

  protected def render(json: JValue): String
}
