/*
 * Copyright 2013 http4s.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.http4s

import cats.data.NonEmptyList
import cats.syntax.all._
import org.typelevel.ci.CIString
import scala.annotation.tailrec
import scala.reflect.ClassTag

sealed trait HeaderKey {
  type HeaderT <: Header

  def name: CIString

  def matchHeader(header: Header): Option[HeaderT]
  final def unapply(header: Header): Option[HeaderT] = matchHeader(header)

  override def toString: String = s"HeaderKey($name)"

  def parse(s: String): ParseResult[HeaderT]
}

object HeaderKey {
  sealed trait Extractable extends HeaderKey {
    def from(headers: Headers): Option[HeaderT]
    final def unapply(headers: Headers): Option[HeaderT] = from(headers)
  }

  /** Represents a Header that should not be repeated.
    */
  trait Singleton extends Extractable {
    final def from(headers: Headers): Option[HeaderT] =
      headers.collectFirst(Function.unlift(matchHeader))
  }

  /** Represents a header key whose multiple headers can be combined by joining
    * their values with a comma.  See RFC 2616, Section 4.2.
    */
  trait Recurring extends Extractable {
    type HeaderT <: Header.Recurring
    type ValueT = RecurringV[HeaderT]
    type GetT = Option[HeaderT]

    def apply(values: NonEmptyList[ValueT]): HeaderT

    def apply(first: ValueT, more: ValueT*): HeaderT =
      apply(NonEmptyList(first, more.toList))

    def from(headers: Headers): Option[HeaderT] = {
      @tailrec def loop(
          hs: Headers,
          acc: NonEmptyList[ValueT]): NonEmptyList[ValueT] =
        if (hs.nonEmpty)
          matchHeader(hs.toList.head) match {
            case Some(header) =>
              loop(Headers(hs.toList.tail), acc.concatNel(header.values[ValueT]))
            case None =>
              loop(Headers(hs.toList.tail), acc)
          }
        else acc
      @tailrec def start(hs: Headers): Option[HeaderT] =
        if (hs.nonEmpty)
          matchHeader(hs.toList.head) match {
            case Some(header) =>
              Some(apply(loop(Headers(hs.toList.tail), header.values.widen[ValueT])))
            case None => start(Headers(hs.toList.tail))
          }
        else None
      start(headers)
    }
  }

  type RecurringV[R <: Header.Recurring.Aux[_]] = R match {
    case Header.Recurring.Aux[v] => v
  }

  private[http4s] abstract class Internal[T <: Header: ClassTag] extends HeaderKey {
    type HeaderT = T
    val name = CIString(
      getClass.getName
        .split("\\.")
        .last
        .replaceAll("\\$minus", "-")
        .split("\\$")
        .last
        .replace("\\$$", ""))
    private val runtimeClass = implicitly[ClassTag[HeaderT]].runtimeClass
    override def matchHeader(header: Header): Option[HeaderT] =
      header match {
        case h if runtimeClass.isInstance(h) =>
          Some(header.asInstanceOf[HeaderT])
        case Header.Raw(_, _) if name == header.name && runtimeClass.isInstance(header.parsed) =>
          Some(header.parsed.asInstanceOf[HeaderT])
        case _ =>
          None
      }
  }

  private[http4s] trait StringKey extends Singleton {
    type HeaderT = Header
    override def matchHeader(header: Header): Option[HeaderT] =
      if (header.name == name) Some(header)
      else None
  }

  private[http4s] trait Default extends Internal[Header] with StringKey {
    override type HeaderT = Header

    override def parse(s: String): ParseResult[Header] =
      ParseResult.success(Header.Raw(name, s))
  }
}
