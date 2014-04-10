package org.http4s

import org.http4s.util.{CaseInsensitiveString, Writer}
import scala.concurrent.duration.Duration
import util.string._
import org.http4s.parser.HttpParser

sealed trait CacheDirective extends HttpValue {
  protected type Self = CacheDirective
  def name: CaseInsensitiveString
  override def toString = value
  def render[W <: Writer](writer: W) = writer ~ name
}

/**
 * A registry of cache-directives, as listed in
 * http://www.iana.org/assignments/http-cache-directives/http-cache-directives.xhtml
 */
object CacheDirective extends HttpValueCompanion {
  protected type Value = CacheDirective

  def fromString(s: String) = (new HttpParser.CacheControlParser(s)).CacheDirective.run().get

  private[http4s] case class CustomCacheDirective(override val name: CaseInsensitiveString, argument: Option[String] = None)
    extends CacheDirective
  {
    override def render[W <: Writer](writer: W): writer.type =
      writer ~ name ~ argument.fold("")(arg => s"""="${arg}"""")
  }

  private[http4s] trait HasDuration { this: CacheDirective =>
    def deltaSeconds: Duration
    override def render[W <: Writer](writer: W): writer.type =
      writer ~ name ~ "=" ~ deltaSeconds.toSeconds
  }

  private[http4s] trait HasFieldNames { this: CacheDirective =>
    def fieldNames: Seq[CaseInsensitiveString]
    override def render[W <: Writer](writer: W): writer.type =
      writer ~ name ~ (if (fieldNames.isEmpty) "" else fieldNames.mkString("=\"", ",", "\""))
  }

  case class `max-age`(deltaSeconds: Duration) extends CacheDirective with HasDuration {
    final val name = "max-age".ci
  }

  case class `max-stale`(deltaSeconds: Option[Duration] = None) extends CacheDirective {
    final val name = "max-stale".ci
    override def render[W <: Writer](writer: W): writer.type =
      writer ~ name ~ deltaSeconds.fold("")(ds => s"=${ds.toSeconds}")
  }

  case class `min-fresh`(deltaSeconds: Duration) extends CacheDirective with HasDuration {
    final val name = "min-fresh".ci
  }

  case object `must-revalidate` extends CacheDirective {
    final val name = "must-revalidate".ci
  }

  case class `no-cache`(fieldNames: Seq[CaseInsensitiveString] = Seq.empty) extends CacheDirective with HasFieldNames {
    final val name = "no-cache".ci
  }

  case object `no-store` extends CacheDirective {
    final val name = "no-store".ci
  }

  case object `no-transform` extends CacheDirective {
    final val name = "no-transform".ci
  }

  case object `only-if-cached` extends CacheDirective {
    final val name = "only-if-cached".ci
  }

  case class `private`(fieldNames: Seq[CaseInsensitiveString] = Nil) extends CacheDirective with HasFieldNames {
    final val name = "private".ci
  }

  case object `proxy-revalidate` extends CacheDirective {
    final val name = "proxy-revalidate".ci
  }

  case object public extends CacheDirective {
    final val name = "public".ci
  }

  case class `s-maxage`(deltaSeconds: Duration) extends CacheDirective with HasDuration {
    final val name = "s-maxage".ci
  }

  case class `stale-if-error`(deltaSeconds: Duration) extends CacheDirective with HasDuration {
    final val name = "stale-if-error".ci
  }

  case class `stale-while-revalidate`(deltaSeconds: Duration) extends CacheDirective with HasDuration {
    final val name = "stale-while-revalidate".ci
  }
}