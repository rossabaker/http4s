package org.http4s.json4s

import jawn.Facade
import jawn.support.json4s.Parser.facade
import org.http4s.json.JawnJsonReader
import org.json4s.JValue

trait Json4sReader extends JawnJsonReader[JValue] {
  override protected implicit def jawnFacade: Facade[JValue] = facade
}

object Json4sReader extends Json4sReader
