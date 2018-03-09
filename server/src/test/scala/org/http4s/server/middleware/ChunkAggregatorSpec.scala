package org.http4s.server.middleware

import cats.arrow.FunctionK
import cats.data.{NonEmptyList, OptionT}
import cats.effect.IO
import cats.implicits._
import fs2._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers._
import org.scalacheck._
import org.specs2.matcher.MatchResult

class ChunkAggregatorSpec extends Http4sSpec {

  val transferCodingGen: Gen[Seq[TransferCoding]] =
    Gen.someOf(
      Seq(
        TransferCoding.compress,
        TransferCoding.deflate,
        TransferCoding.gzip,
        TransferCoding.identity))
  implicit val transferCodingArbitrary = Arbitrary(transferCodingGen.map(_.toList))

  "ChunkAggregator" should {
    def checkResponse(body: EntityBody[IO], transferCodings: List[TransferCoding])(
        responseCheck: Response[IO] => MatchResult[Any]): MatchResult[Any] = {
      val service: Http[IO] = Http { _ =>
        Ok(body, `Transfer-Encoding`(NonEmptyList(TransferCoding.chunked, transferCodings)))
          .map(_.removeHeader(`Content-Length`))
      }

      ChunkAggregator(service)(FunctionK.id).run(Request()).unsafeRunSync must be like {
        case response =>
          response.status must_== Ok
          responseCheck(response)
      }
    }

    "handle an empty body" in {
      checkResponse(EmptyBody, Nil) { response =>
        response.contentLength must beNone
        response.body.compile.toVector.unsafeRunSync() must_=== Vector.empty
      }
    }

    "handle a None" in {
      val service: HttpPartial[IO] = HttpPartial.empty
      ChunkAggregator(service)(OptionT.liftK).run(Request()).value must returnValue(None)
    }

    "handle chunks" in {
      prop { (chunks: NonEmptyList[Chunk[Byte]], transferCodings: List[TransferCoding]) =>
        val totalChunksSize = chunks.foldMap(_.size)
        checkResponse(chunks.map(Stream.chunk[Byte]).reduceLeft(_ ++ _), transferCodings) {
          response =>
            if (totalChunksSize > 0) {
              response.contentLength must beSome(totalChunksSize.toLong)
              response.headers.get(`Transfer-Encoding`).map(_.values) must_=== NonEmptyList
                .fromList(transferCodings)
            }
            response.body.compile.toVector.unsafeRunSync() must_=== chunks.foldMap(_.toVector)
        }
      }
    }
  }

}
