package org.http4s

import org.http4s.util.Renderable

trait HttpValue extends Renderable {
  protected type Self <: HttpValue

  override def toString = value
}

trait HttpValueCompanion {
  protected type Value <: HttpValue

  def fromString(s: String): Value
}
