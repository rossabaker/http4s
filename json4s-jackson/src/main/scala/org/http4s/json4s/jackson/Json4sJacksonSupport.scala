package org.http4s.json4s.jackson

import org.http4s.json.JsonSupport
import org.http4s.json4s.Json4sReader
import org.json4s.JValue

trait Json4sJacksonSupport extends JsonSupport[JValue] with Json4sReader with Json4sJacksonWriter

object Json4sJacksonSupport
