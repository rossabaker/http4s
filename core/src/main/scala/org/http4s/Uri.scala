package org.http4s

import org.http4s.util.CaseInsensitiveString

import Uri._
import org.http4s.parser.RequestUriParser
import java.nio.charset.Charset
import scalaz._
import scalaz.std.anyVal._
import org.http4s.Uri.Authority
import scalaz.Free._
import org.http4s.Uri.Authority
import scalaz.Trampoline
import org.http4s.Uri.Authority

case class Uri (
  scheme: Option[CaseInsensitiveString] = None,
  authority: Option[Authority] = None,
  path: Path = Uri.Path.empty,
  query: Option[Query] = None,
  fragment: Option[Fragment] = None
) {
  def withPath(path: Path): Uri = copy(path = path)

  def host: Option[Host] = authority.map(_.host)
  def port: Option[Int] = authority.flatMap(_.port)
}

object Uri {
  def fromString(s: String): Uri = (new RequestUriParser(s, Charset.forName("UTF-8"))).RequestUri.run().get

  type Scheme = CaseInsensitiveString

  case class Authority(
    userInfo: Option[UserInfo] = None,
    host: Host = "localhost".ci,
    port: Option[Int] = None
  )

  type Host = CaseInsensitiveString
  type UserInfo = String

  sealed class Path private (private val self: String) {
    def :+(s: String): Path = new Path(self + s)

    def ++(path: Path): Path = {
      val concat = self + path.self
      new Path(concat)
    }

    def startsWith(s: String) = take(s.length).toString.startsWith(s)

    def splitAt(n: Int): (Path, Path) = {
      val (l, r) = self.splitAt(n)
      (new Path(l), new Path(r))
    }

    def take(n: Int): Path = new Path(self.take(n))

    def drop(n: Int): Path = new Path(self.drop(n))

    override lazy val toString: String = self

    lazy val segments: Seq[String] = self.split('/')
  }

  object Path {
    private implicit val sizer: Reducer[String, Int] = UnitReducer(_.length)

    def apply(segments: String*): Path = fromSegments(segments)

    def fromSegments(segments: Seq[String]): Path = new Path(segments.foldLeft(new StringBuilder)(_ append _).toString)

    val empty = fromSegments(Seq.empty)
    val / : Path = fromSegments(Seq("/"))
  }

  type Query = String
  type Fragment = String

  val / : Uri = Uri(path = Path./)
}
