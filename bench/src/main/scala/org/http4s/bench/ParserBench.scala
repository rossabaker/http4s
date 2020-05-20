/*
 * Copyright 2013-2020 http4s.org
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.http4s.bench

import atto._, Atto._
import cats.implicits._
import java.lang.String
import org.openjdk.jmh.annotations.Benchmark
import org.http4s.internal.parboiled2.{Parser => PbParser, _}

// Json AST
sealed trait JValue
case object JNull extends JValue
final case class JBoolean(value: Boolean) extends JValue
final case class JString(value: String) extends JValue
final case class JNumber(value: Double) extends JValue
final case class JArray(values: List[JValue]) extends JValue
final case class JObject(values: List[(String, JValue)]) extends JValue

object JValue {
  // Invariant constructors
  def jNull: JValue = JNull
  def jBoolean(value: Boolean): JValue = JBoolean(value)
  def jString(value: String): JValue = JString(value)
  def jNumber(value: Double): JValue = JNumber(value)
  def jNumber(value: String): JValue = JNumber(new scala.collection.StringOps(value).toDouble)
  def jArray(values: List[JValue]): JValue = JArray(values)
  def jObject(values: List[(String, JValue)]): JValue = JObject(values)
}

object attobench extends Whitespace {
  import JValue._

  // Bracketed, comma-separated sequence, internal whitespace allowed
  def seq[A](open: Char, p: Parser[A], close: Char): Parser[List[A]] =
    char(open).t ~> sepByT(p, char(',')) <~ char(close)

  // Colon-separated pair, internal whitespace allowed
  lazy val pair: Parser[(String, JValue)] =
    pairByT(stringLiteral, char(':'), jexpr)

  // Json Expression
  lazy val jexpr: Parser[JValue] = delay {
    stringLiteral -| jString |
      seq('{', pair, '}') -| jObject |
      seq('[', jexpr, ']') -| jArray |
      double -| jNumber |
      string("null") >| jNull |
      string("true") >| jBoolean(true) |
      string("false") >| jBoolean(false)
  }

}

// Some extre combinators and syntax for coping with whitespace. Something like this might be
// useful in core but it needs some thought.
trait Whitespace {

  // Syntax for turning a parser into one that consumes trailing whitespace
  implicit class TokenOps[A](self: Parser[A]) {
    def t: Parser[A] =
      self <~ takeWhile(c => c.isSpaceChar || c === '\n')
  }

  // Delimited list
  def sepByT[A](a: Parser[A], b: Parser[_]): Parser[List[A]] =
    sepBy(a.t, b.t)

  // Delimited pair, internal whitespace allowed
  def pairByT[A, B](a: Parser[A], delim: Parser[_], b: Parser[B]): Parser[(A, B)] =
    pairBy(a.t, delim.t, b)

}

object JsonTest {

  val text =
    """
[
    {
        "id": 0,
        "guid": "24e3bb35-f5da-47ac-910e-1aa6568938c8",
        "isActive": true,
        "balance": "$1,880.00",
        "picture": "http://placehold.it/32x32",
        "age": 21,
        "name": "Gallegos Rich",
        "gender": "male",
        "company": "ZORROMOP",
        "email": "gallegosrich@zorromop.com",
        "phone": "+1 (996) 457-2721",
        "address": "599 Dupont Street, Healy, New Mexico, 5501",
        "about": "Adipisicing magna Lorem excepteur non sint aute sint anim exercitation ullamco voluptate eu dolor non. Sint fugiat incididunt consequat aliqua amet elit. Sint cillum nostrud aliqua minim culpa est.\r\n",
        "registered": "2014-03-21T00:07:44 +07:00",
        "latitude": 81,
        "longitude": -133,
        "tags": [
            "mollit",
            "anim",
            "ad",
            "laboris",
            "quis",
            "magna",
            "reprehenderit"
        ],
        "friends": [
            {
                "id": 0,
                "name": "Stacy Burks"
            },
            {
                "id": 1,
                "name": "Higgins Weber"
            },
            {
                "id": 2,
                "name": "Elvira Blair"
            }
        ],
        "greeting": "Hello, Gallegos Rich! You have 4 unread messages.",
        "favoriteFruit": "banana"
    },
    {
        "id": 1,
        "guid": "68ff4ed1-54b6-44b0-a3cc-5ac4a0785f28",
        "isActive": false,
        "balance": "$2,576.00",
        "picture": "http://placehold.it/32x32",
        "age": 36,
        "name": "Kelley Cooke",
        "gender": "male",
        "company": "LIMOZEN",
        "email": "kelleycooke@limozen.com",
        "phone": "+1 (858) 479-3389",
        "address": "925 Howard Place, Wildwood, Pennsylvania, 992",
        "about": "Amet aliqua ex in occaecat. Nostrud voluptate dolore elit deserunt enim enim dolor excepteur non enim. In commodo aute Lorem et nisi excepteur id nisi amet nisi. Ut Lorem consectetur id culpa labore tempor adipisicing eu ea quis. Qui aliqua eiusmod aute cupidatat tempor commodo incididunt amet enim eiusmod. Non qui est deserunt qui minim cillum commodo magna irure consequat.\r\n",
        "registered": "2014-04-11T07:27:15 +07:00",
        "latitude": 7,
        "longitude": 78,
        "tags": [
            "minim",
            "duis",
            "duis",
            "minim",
            "sit",
            "ea",
            "incididunt"
        ],
        "friends": [
            {
                "id": 0,
                "name": "Brandi Trevino"
            },
            {
                "id": 1,
                "name": "Fuentes Daugherty"
            },
            {
                "id": 2,
                "name": "Gillespie Cash"
            }
        ],
        "greeting": "Hello, Kelley Cooke! You have 1 unread messages.",
        "favoriteFruit": "strawberry"
    },
    {
        "id": 2,
        "guid": "201e59af-1b60-4a61-9d14-fb6db351b049",
        "isActive": false,
        "balance": "$1,339.00",
        "picture": "http://placehold.it/32x32",
        "age": 27,
        "name": "Hope Delacruz",
        "gender": "female",
        "company": "COMCUBINE",
        "email": "hopedelacruz@comcubine.com",
        "phone": "+1 (908) 467-2395",
        "address": "216 Logan Street, Yardville, Vermont, 8018",
        "about": "Et elit proident ut aute ea qui aute id elit. Sunt aliquip ad ipsum sit ut amet do nulla. Lorem aliquip voluptate Lorem veniam. Ea id reprehenderit et enim aliquip. Elit voluptate magna amet nulla excepteur aliquip. Mollit fugiat veniam Lorem dolore nulla sint et pariatur tempor.\r\n",
        "registered": "2014-02-13T00:55:00 +08:00",
        "latitude": 24,
        "longitude": -54,
        "tags": [
            "mollit",
            "nostrud",
            "proident",
            "aliquip",
            "aliquip",
            "do",
            "excepteur"
        ],
        "friends": [
            {
                "id": 0,
                "name": "Durham Dunlap"
            },
            {
                "id": 1,
                "name": "Penny Dyer"
            },
            {
                "id": 2,
                "name": "Louella Warren"
            }
        ],
        "greeting": "Hello, Hope Delacruz! You have 9 unread messages.",
        "favoriteFruit": "apple"
    },
    {
        "id": 3,
        "guid": "1fe07ccf-31d8-4a40-a5cb-8d29cce48630",
        "isActive": false,
        "balance": "$2,556.00",
        "picture": "http://placehold.it/32x32",
        "age": 34,
        "name": "Lopez Cross",
        "gender": "male",
        "company": "QUONATA",
        "email": "lopezcross@quonata.com",
        "phone": "+1 (888) 483-2717",
        "address": "222 Visitation Place, Katonah, Oklahoma, 833",
        "about": "Aute minim exercitation sint sunt nisi proident. Adipisicing in duis officia ea qui aute. Sit officia duis consectetur aute cupidatat. Cillum reprehenderit elit veniam elit labore non ex officia. Elit est et nostrud ea minim mollit pariatur cillum fugiat magna nisi voluptate cillum officia.\r\n",
        "registered": "2014-02-11T00:02:49 +08:00",
        "latitude": 77,
        "longitude": -101,
        "tags": [
            "commodo",
            "eu",
            "nulla",
            "Lorem",
            "laboris",
            "exercitation",
            "incididunt"
        ],
        "friends": [
            {
                "id": 0,
                "name": "Maritza Potter"
            },
            {
                "id": 1,
                "name": "Schmidt Todd"
            },
            {
                "id": 2,
                "name": "Chasity Carroll"
            }
        ],
        "greeting": "Hello, Lopez Cross! You have 5 unread messages.",
        "favoriteFruit": "banana"
    },
    {
        "id": 4,
        "guid": "a44846f7-7204-445d-b11f-80c020262165",
        "isActive": false,
        "balance": "$2,388.00",
        "picture": "http://placehold.it/32x32",
        "age": 29,
        "name": "Valentine Nguyen",
        "gender": "male",
        "company": "ECRATER",
        "email": "valentinenguyen@ecrater.com",
        "phone": "+1 (927) 579-3317",
        "address": "469 Tapscott Avenue, Titanic, Kentucky, 5275",
        "about": "Amet ut veniam ullamco voluptate. Qui non aliqua irure ipsum aute. Velit aute deserunt est Lorem velit fugiat consequat ullamco cupidatat culpa eu. Aute sunt et esse laboris enim dolore deserunt veniam aliquip consectetur. Consectetur eiusmod laboris officia proident amet ut nostrud nostrud tempor veniam fugiat.\r\n",
        "registered": "2014-04-11T03:12:48 +07:00",
        "latitude": -82,
        "longitude": 22,
        "tags": [
            "quis",
            "eu",
            "anim",
            "aliquip",
            "ullamco",
            "occaecat",
            "dolor"
        ],
        "friends": [
            {
                "id": 0,
                "name": "Dickson Santos"
            },
            {
                "id": 1,
                "name": "Tracey Mckenzie"
            },
            {
                "id": 2,
                "name": "Avila Terry"
            }
        ],
        "greeting": "Hello, Valentine Nguyen! You have 10 unread messages.",
        "favoriteFruit": "banana"
    },
    {
        "id": 5,
        "guid": "50264b3b-0395-429b-8ec9-8c41821e84c4",
        "isActive": true,
        "balance": "$1,860.00",
        "picture": "http://placehold.it/32x32",
        "age": 32,
        "name": "Holland Gibson",
        "gender": "male",
        "company": "PEARLESSA",
        "email": "hollandgibson@pearlessa.com",
        "phone": "+1 (953) 442-3713",
        "address": "909 Cass Place, Lithium, Alabama, 3836",
        "about": "Sit pariatur exercitation tempor labore est. Incididunt fugiat pariatur amet in pariatur do magna pariatur id. Ad adipisicing est ad tempor reprehenderit aliqua quis esse nulla dolor. Magna consequat dolore culpa dolor amet excepteur deserunt minim consequat non cupidatat aliqua enim.\r\n",
        "registered": "2014-03-03T12:16:22 +08:00",
        "latitude": -47,
        "longitude": 66,
        "tags": [
            "duis",
            "cillum",
            "irure",
            "ut",
            "consequat",
            "sint",
            "laboris"
        ],
        "friends": [
            {
                "id": 0,
                "name": "Hallie Thomas"
            },
            {
                "id": 1,
                "name": "Adele Joseph"
            },
            {
                "id": 2,
                "name": "Gayle Poole"
            }
        ],
        "greeting": "Hello, Holland Gibson! You have 10 unread messages.",
        "favoriteFruit": "banana"
    },
    {
        "id": 6,
        "guid": "ddff18fc-6d88-4200-bf23-bf68b711eada",
        "isActive": true,
        "balance": "$2,160.00",
        "picture": "http://placehold.it/32x32",
        "age": 27,
        "name": "Gibson Lane",
        "gender": "male",
        "company": "COMVEYOR",
        "email": "gibsonlane@comveyor.com",
        "phone": "+1 (990) 599-3696",
        "address": "399 Huntington Street, Brownsville, Florida, 7576",
        "about": "Magna anim enim aute proident duis sint. Culpa sint ipsum elit consectetur et. Quis nostrud occaecat consequat sint cillum ea eiusmod velit ex fugiat aliqua reprehenderit non minim. Anim ad nisi et Lorem ullamco nulla eiusmod qui pariatur qui laborum deserunt cupidatat.\r\n",
        "registered": "2014-03-27T14:50:16 +07:00",
        "latitude": 8,
        "longitude": 86,
        "tags": [
            "ad",
            "id",
            "ad",
            "duis",
            "commodo",
            "consectetur",
            "Lorem"
        ],
        "friends": [
            {
                "id": 0,
                "name": "Doreen Macdonald"
            },
            {
                "id": 1,
                "name": "Geraldine Buchanan"
            },
            {
                "id": 2,
                "name": "Imelda Mclaughlin"
            }
        ],
        "greeting": "Hello, Gibson Lane! You have 5 unread messages.",
        "favoriteFruit": "strawberry"
    }
]

  """.trim

}

/**
  * This is a feature-complete JSON parser implementation that almost directly
  * models the JSON grammar presented at http://www.json.org as a parboiled2 PEG parser.
  */
class JsonParser(val input: ParserInput) extends PbParser with StringBuilding {
  import JValue._
  import scala.annotation.switch

  import CharPredicate.{Digit, Digit19, HexDigit}

  // the root rule
  def Json = rule(WhiteSpace ~ Value ~ EOI)

  def JsonObject: Rule1[JValue] =
    rule {
      ws('{') ~ zeroOrMore(Pair).separatedBy(ws(',')) ~ ws('}') ~> (
        (fields: Seq[(String, JValue)]) => jObject(fields.toList))
    }

  def Pair = rule(JsonStringUnwrapped ~ ws(':') ~ Value ~> ((_, _)))

  def Value: Rule1[JValue] =
    rule {
      // as an optimization of the equivalent rule:
      //   JsonString | JsonNumber | JsonObject | JsonArray | JsonTrue | JsonFalse | JsonNull
      // we make use of the fact that one-char lookahead is enough to discriminate the cases
      run {
        (cursorChar: @switch) match {
          case '"' => JsonString
          case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '-' => JsonNumber
          case '{' => JsonObject
          case '[' => JsonArray
          case 't' => JsonTrue
          case 'f' => JsonFalse
          case 'n' => JsonNull
          case _ => MISMATCH
        }
      }
    }

  def JsonString = rule(JsonStringUnwrapped ~> (jString(_)))

  def JsonStringUnwrapped = rule('"' ~ clearSB() ~ Characters ~ ws('"') ~ push(sb.toString))

  def JsonNumber =
    rule {
      capture(Integer ~ optional(Frac) ~ optional(Exp)) ~> { s: String => jNumber(s) } ~ WhiteSpace
    }

  def JsonArray =
    rule {
      ws('[') ~ zeroOrMore(Value).separatedBy(ws(',')) ~ ws(']') ~> { s: Seq[JValue] =>
        jArray(s.toList)
      }
    }

  def Characters = rule(zeroOrMore(NormalChar | '\\' ~ EscapedChar))

  def NormalChar = rule(!QuoteBackslash ~ ANY ~ appendSB())

  def EscapedChar =
    rule(
      QuoteSlashBackSlash ~ appendSB()
        | 'b' ~ appendSB('\b')
        | 'f' ~ appendSB('\f')
        | 'n' ~ appendSB('\n')
        | 'r' ~ appendSB('\r')
        | 't' ~ appendSB('\t')
        | Unicode ~> { code => sb.append(code.asInstanceOf[Char]); () }
    )

  def Unicode =
    rule {
      'u' ~ capture(HexDigit ~ HexDigit ~ HexDigit ~ HexDigit) ~> (java.lang.Integer
        .parseInt(_, 16))
    }

  def Integer = rule(optional('-') ~ (Digit19 ~ Digits | Digit))

  def Digits = rule(oneOrMore(Digit))

  def Frac = rule("." ~ Digits)

  def Exp = rule(ignoreCase('e') ~ optional(anyOf("+-")) ~ Digits)

  def JsonTrue = rule("true" ~ WhiteSpace ~ push(jBoolean(true)))

  def JsonFalse = rule("false" ~ WhiteSpace ~ push(jBoolean(false)))

  def JsonNull = rule("null" ~ WhiteSpace ~ push(jNull))

  def WhiteSpace = rule(zeroOrMore(WhiteSpaceChar))

  def ws(c: Char) = rule(c ~ WhiteSpace)

  val WhiteSpaceChar = CharPredicate(" \n\r\t\f")
  val QuoteBackslash = CharPredicate("\"\\")
  val QuoteSlashBackSlash = QuoteBackslash ++ "/"
}

object JsonParse {
  import fastparse._, JValue._, MultiLineWhitespace._

  def stringChars(c: Char) = c != '\"' && c != '\\'

  def space[_: P] = P(CharsWhileIn(" \r\n", 0))
  def digits[_: P] = P(CharsWhileIn("0-9"))
  def exponent[_: P] = P(CharIn("eE") ~ CharIn("+\\-").? ~ digits)
  def fractional[_: P] = P("." ~ digits)
  def integral[_: P] = P("0" | CharIn("1-9") ~ digits.?)

  def number[_: P] =
    P(CharIn("+\\-").? ~ integral ~ fractional.? ~ exponent.?).!.map(x => jNumber(x.toDouble))

  def `null`[_: P] = P("null").map(_ => jNull)
  def `false`[_: P] = P("false").map(_ => jBoolean(false))
  def `true`[_: P] = P("true").map(_ => jBoolean(true))

  def hexDigit[_: P] = P(CharIn("0-9a-fA-F"))
  def unicodeEscape[_: P] = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)
  def escape[_: P] = P("\\" ~ (CharIn("\"/\\\\bfnrt") | unicodeEscape))

  def strChars[_: P] = P(CharsWhile(stringChars))
  def string[_: P] =
    P(space ~ "\"" ~/ (strChars | escape).rep.! ~ "\"").map(JString.apply)

  def array[_: P] =
    P("[" ~/ jsonExpr.rep(sep = ","./) ~ space ~ "]").map(s => jArray(s.toList))

  def pair[_: P] = P(string.map(_.value) ~/ ":" ~/ jsonExpr)

  def obj[_: P] =
    P("{" ~/ pair.rep(sep = ","./) ~ space ~ "}").map(s => jObject(s.toList))

  def jsonExpr[_: P]: P[JValue] =
    P(
      space ~ (obj | array | string | `true` | `false` | `null` | number) ~ space
    )
}

class ParserBench {
  @Benchmark
  def testAtto: atto.ParseResult[JValue] =
    attobench.jexpr.parseOnly(JsonTest.text)

  @Benchmark
  def testParboiled: scala.util.Try[JValue] =
    new JsonParser(JsonTest.text).Json.run()

  @Benchmark
  def testFastparse: fastparse.Parsed[JValue] =
    fastparse.parse(JsonTest.text, JsonParse.jsonExpr(_))
}
