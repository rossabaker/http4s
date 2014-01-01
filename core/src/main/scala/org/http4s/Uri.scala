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

  /**
   * Inspired by scalaz.Cord.
   */
  sealed class Path private (private val self: FingerTree[Int, String]) {
    def :+(s: String): Path = new Path(self :+ s)

    def ++(path: Path): Path = {
      val concat = self <++> path.self
      new Path(concat)
    }

    def startsWith(s: String) = take(s.length).toString.startsWith(s)

    def splitAt(n: Int): (Path, Path) = {
      val (l, mid, r) = self.split1(_ > n)
      val (midl, midr) = mid.splitAt(n - l.measure)
      (new Path(l :+ midl), new Path(midr +: r))
    }

    def take(n: Int): Path = splitAt(n)._1

    def drop(n: Int): Path = splitAt(n)._2

    override lazy val toString: String = {
      import scalaz.syntax.foldable._
      import Free._
      val sb = new StringBuilder(self.measure)
      val t = self.traverse_[Trampoline](x => Trampoline.delay(sb ++= x))
      t.run
      sb.toString
    }

    lazy val segments: Seq[String] = self.iterator.filterNot(_ == "/").toSeq
  }

  object Path {
    private implicit val sizer: Reducer[String, Int] = UnitReducer(_.length)

    def apply(segments: String*): Path = fromSegments(segments)

    def fromSegments(segments: Seq[String]): Path =
      new Path(segments.foldLeft(FingerTree.empty[Int, String](sizer))(_ :+ _))

    val empty = fromSegments(Seq.empty)
    val / : Path = fromSegments(Seq("/"))
  }

  type Query = String
  type Fragment = String

  val / : Uri = Uri(path = Path./)
}
