package com.example.http4s.blaze.demo.server

import cats.effect.Effect
import cats.syntax.semigroupk._ // For <+>
import com.example.http4s.blaze.demo.server.endpoints._
import com.example.http4s.blaze.demo.server.endpoints.auth.{
  BasicAuthHttpEndpoint,
  GitHubHttpEndpoint
}
import com.example.http4s.blaze.demo.server.service.{FileService, GitHubService}
import fs2.Scheduler
import org.http4s.HttpPartial
import org.http4s.client.Client
import org.http4s.server.HttpMiddleware
import org.http4s.server.middleware.{AutoSlash, ChunkAggregator, GZip, Timeout}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class Module[F[_]](client: Client[F])(implicit F: Effect[F], S: Scheduler) {

  private val fileService = new FileService[F]

  private val gitHubService = new GitHubService[F](client)

  def middleware: HttpMiddleware[F] = { (service: HttpPartial[F]) =>
    GZip(service)(F)
  }.compose { service =>
    AutoSlash(service)(F)
  }

  val fileHttpEndpoint: HttpPartial[F] =
    new FileHttpEndpoint[F](fileService).service

  val nonStreamFileHttpEndpoint = ChunkAggregator(fileHttpEndpoint)

  private val hexNameHttpEndpoint: HttpPartial[F] =
    new HexNameHttpEndpoint[F].service

  private val compressedEndpoints: HttpPartial[F] =
    middleware(hexNameHttpEndpoint)

  private val timeoutHttpEndpoint: HttpPartial[F] =
    new TimeoutHttpEndpoint[F].service

  private val timeoutEndpoints: HttpPartial[F] =
    Timeout(1.second)(timeoutHttpEndpoint)

  private val mediaHttpEndpoint: HttpPartial[F] =
    new JsonXmlHttpEndpoint[F].service

  private val multipartHttpEndpoint: HttpPartial[F] =
    new MultipartHttpEndpoint[F](fileService).service

  private val gitHubHttpEndpoint: HttpPartial[F] =
    new GitHubHttpEndpoint[F](gitHubService).service

  val basicAuthHttpEndpoint: HttpPartial[F] =
    new BasicAuthHttpEndpoint[F].service

  val httpServices: HttpPartial[F] = (
    compressedEndpoints <+> timeoutEndpoints
      <+> mediaHttpEndpoint <+> multipartHttpEndpoint
      <+> gitHubHttpEndpoint
  )

}
