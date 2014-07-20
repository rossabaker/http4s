package org.http4s.json4s.native

import org.http4s.json4s.Json4sWriter
import org.json4s.JValue
import org.json4s.native._

trait Json4sNativeWriter extends Json4sWriter {
  override protected def render(json: JValue): String = compactJson(renderJValue(json))
}
