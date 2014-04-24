package org.http4s.middleware

import org.http4s._
import scalaz.concurrent.Task
import scalaz.stream.Process1
import scodec.bits.ByteVector
import scalaz.stream.Process._
import scalaz.stream.Process.Emit
import org.http4s.Response
import scala.util.control.NoStackTrace

/**
 * Limits an HTTP body.
 */
object maxBodySize {
  def apply(maxSize: Int, tooLargeResponse: Task[Response] = DefaultEntityTooLargeResponse)(service: HttpService): HttpService = new HttpService {
    def apply(req: Request): Task[Response] =
      // First, check the header.  If they said it's too long, believe it.
      if (isTooLarge(req, maxSize)) tooLargeResponse
      // Now trust, but verify.
      else service(limitBody(req, maxSize)).handleWith {
        case EntityTooLarge => tooLargeResponse
      }

    def isDefinedAt(req: Request): Boolean = service.isDefinedAt(req)

    override def applyOrElse[A1 <: Request, B1 >: Task[Response]](req: A1, default: (A1) => B1): B1 = {
      if (isTooLarge(req, maxSize))
        if (service.isDefinedAt(req)) tooLargeResponse
        else default(req)
      else service.lift(limitBody(req, maxSize)).getOrElse(default(req))
    }
  }

  val DefaultEntityTooLargeResponse = Status.RequestEntityTooLarge()

  private def isTooLarge(req: Request, maxSize: Int) = req.contentLength.fold(false)(_ > maxSize)

  private def limitBody(req: Request, maxSize: Int): Request =
    req.withBody(req.body |> takeBytes(maxSize))

  private def takeBytes(n: Int): Process1[ByteVector, ByteVector] = {
    import Process._
    def go(taken: Int, chunk: ByteVector): Process1[ByteVector, ByteVector] = {
      val sz = taken + chunk.length
      if (sz > n) fail(EntityTooLarge)
      else Emit(Seq(chunk), await(Get[ByteVector])(go(sz, _)))
    }
    await(Get[ByteVector])(go(0, _))
  }

  private case object EntityTooLarge extends Exception with NoStackTrace
}
