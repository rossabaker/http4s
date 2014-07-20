package org.http4s.json4s.native

import org.http4s.json.JsonSupport
import org.http4s.json4s.Json4sReader
import org.json4s.JValue

trait Json4sNativeSupport extends JsonSupport[JValue] with Json4sReader with Json4sNativeWriter

object Json4sNativeSupport extends Json4sNativeSupport
