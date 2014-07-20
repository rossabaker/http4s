package org.http4s.json4s.jackson

import org.http4s.json4s.Json4sWriter
import org.json4s.JValue
import org.json4s.jackson._

trait Json4sJacksonWriter extends Json4sWriter {
  override protected def render(json: JValue): String = compactJson(renderJValue(json))
}
