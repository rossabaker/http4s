package com.example.http4s.blaze.demo.server.endpoints

import cats.effect._
import com.example.http4s.blaze.demo.server.service.FileService
import org.http4s._
import org.http4s.dsl.Http4sDsl

class FileHttpEndpoint[F[_]: Sync](fileService: FileService[F]) extends Http4sDsl[F] {

  object DepthQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("depth")

  val service: HttpPartial[F] = HttpPartial {
    case GET -> Root / "dirs" :? DepthQueryParamMatcher(depth) =>
      Ok(fileService.homeDirectories(depth))
  }

}
