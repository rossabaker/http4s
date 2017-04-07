package org.http4s
package client

import java.util.concurrent.atomic.AtomicBoolean

import org.http4s.headers.{Accept, MediaRangeAndQValue}
import org.http4s.Status.Successful

import scala.util.control.NoStackTrace
import java.io.IOException

import cats.effect.IO
import fs2.interop.cats._
import fs2._



/**
  * Contains a [[Response]] that needs to be disposed of to free the underlying
  * HTTP connection.
  * @param response
  * @param dispose
  */
final case class DisposableResponse(response: Response, dispose: IO[Unit]) {
  /**
    * Returns a task to handle the response, safely disposing of the underlying
    * HTTP connection when the task finishes.
    */
  def apply[A](f: Response => IO[A]): IO[A] = {
    val task = try f(response) catch { case e: Throwable => IO.fail(e) }
    task.attempt.flatMap(result => dispose.flatMap( _ => result.fold[IO[A]](IO.fail, IO.now)))
  }
}

/**
  * A [[Client]] submits [[Request]]s to a server and processes the [[Response]].
  *
  * @param open a service to asynchronously return a [[DisposableResponse]] from
  *             a [[Request]].  This is a low-level operation intended for client
  *             implementations and middleware.
  *
  * @param shutdown a IO to shut down this Shutdown this client, closing any
  *                 open connections and freeing resources
  */
final case class Client(open: Service[Request, DisposableResponse], shutdown: IO[Unit]) {
   /** Submits a request, and provides a callback to process the response.
    *
    * @param req The request to submit
    * @param f   A callback for the response to req.  The underlying HTTP connection
    *            is disposed when the returned task completes.  Attempts to read the
    *            response body afterward will result in an error.
    * @return The result of applying f to the response to req
    */
  def fetch[A](req: Request)(f: Response => IO[A]): IO[A] = {
    open.run(req).flatMap(_.apply(f))
  }


  /**
    * Returns this client as a [[Service]].  All connections created by this
    * service are disposed on completion of callback task f.
    *
    * This method effectively reverses the arguments to `fetch`, and is
    * preferred when an HTTP client is composed into a larger Kleisli function,
    * or when a common response callback is used by many call sites.
    */
  def toService[A](f: Response => IO[A]): Service[Request, A] =
    open.flatMapF(_.apply(f))

  /**
    * Returns this client as an [[HttpService]].  It is the responsibility of
    * callers of this service to run the response body to dispose of the
    * underlying HTTP connection.
    *
    * This is intended for use in proxy servers.  `fetch`, `fetchAs`,
    * [[toService]], and [[streaming]] are safer alternatives, as their
    * signatures guarantee disposal of the HTTP connection.
    */
  def toHttpService: HttpService = {
    open.flatMapF{
      case DisposableResponse(response, dispose) => dispose.flatMap(_ => IO.now(response))
    }
  }


  def streaming[A](req: Request)(f: Response => Stream[IO, A]): Stream[IO, A] = {
    Stream.eval(open(req))
      .flatMap {
        case DisposableResponse(response, dispose) =>
          f(response)
          .onFinalize(dispose)
      }
  }


  /**
    * Submits a request and decodes the response on success.  On failure, the
    * status code is returned.  The underlying HTTP connection is closed at the
    * completion of the decoding.
    */
  def expect[A](req: Request)(implicit d: EntityDecoder[A]): IO[A] = {
    val r = if (d.consumes.nonEmpty) {
      val m = d.consumes.toList
      req.putHeaders(Accept(MediaRangeAndQValue(m.head), m.tail.map(MediaRangeAndQValue(_)):_*))
    } else req
    fetch(r) {
      case Successful(resp) =>
        d.decode(resp, strict = false).fold(throw _, identity)
      case failedResponse =>
        IO.fail(UnexpectedStatus(failedResponse.status))
    }
  }

  /**
    * Submits a request and decodes the response, regardless of the status code.
    * The underlying HTTP connection is closed at the completion of the
    * decoding.
    */
  def fetchAs[A](req: Request)(implicit d: EntityDecoder[A]): IO[A] = {
    val r = if (d.consumes.nonEmpty) {
      val m = d.consumes.toList
      req.putHeaders(Accept(MediaRangeAndQValue(m.head), m.tail.map(MediaRangeAndQValue(_)):_*))
    } else req
    fetch(r) { resp =>
      d.decode(resp, strict = false).fold(throw _, identity)
    }
  }

  @deprecated("Use expect", "0.14")
  def prepAs[A](req: Request)(implicit d: EntityDecoder[A]): IO[A] =
    fetchAs(req)(d)

  /** Submits a GET request, and provides a callback to process the response.
    *
    * @param uri The URI to GET
    * @param f A callback for the response to a GET on uri.  The underlying HTTP connection
    *          is disposed when the returned task completes.  Attempts to read the
    *          response body afterward will result in an error.
    * @return The result of applying f to the response to req
    */
  def get[A](uri: Uri)(f: Response => IO[A]): IO[A] =
    fetch(Request(Method.GET, uri))(f)

  /**
    * Submits a request and decodes the response on success.  On failure, the
    * status code is returned.  The underlying HTTP connection is closed at the
    * completion of the decoding.
    */
  def get[A](s: String)(f: Response => IO[A]): IO[A] =
    Uri.fromString(s).fold(IO.fail, uri => get(uri)(f))

  /**
    * Submits a GET request to the specified URI and decodes the response on
    * success.  On failure, the status code is returned.  The underlying HTTP
    * connection is closed at the completion of the decoding.
    */
  def expect[A](uri: Uri)(implicit d: EntityDecoder[A]): IO[A] =
    expect(Request(Method.GET, uri))(d)

  /**
    * Submits a GET request to the URI specified by the String and decodes the
    * response on success.  On failure, the status code is returned.  The
    * underlying HTTP connection is closed at the completion of the decoding.
    */
  def expect[A](s: String)(implicit d: EntityDecoder[A]): IO[A] =
    Uri.fromString(s).fold(IO.fail, expect[A])

  /**
    * Submits a GET request and decodes the response.  The underlying HTTP
    * connection is closed at the completion of the decoding.
    */
  @deprecated("Use expect", "0.14")
  def getAs[A](uri: Uri)(implicit d: EntityDecoder[A]): IO[A] =
    fetchAs(Request(Method.GET, uri))(d)

  @deprecated("Use expect", "0.14")
  def getAs[A](s: String)(implicit d: EntityDecoder[A]): IO[A] =
    Uri.fromString(s).fold(IO.fail, uri => expect[A](uri))

  /** Submits a request, and provides a callback to process the response.
    *
    * @param req A IO of the request to submit
    * @param f A callback for the response to req.  The underlying HTTP connection
    *          is disposed when the returned task completes.  Attempts to read the
    *          response body afterward will result in an error.
    * @return The result of applying f to the response to req
    */
  def fetch[A](req: IO[Request])(f: Response => IO[A]): IO[A] =
    req.flatMap(fetch(_)(f))

  def expect[A](req: IO[Request])(implicit d: EntityDecoder[A]): IO[A] =
    req.flatMap(expect(_)(d))

  /**
    * Submits a request and decodes the response, regardless of the status code.
    * The underlying HTTP connection is closed at the completion of the
    * decoding.
    */
  @deprecated("Use expect", "0.14")
  def fetchAs[A](req: IO[Request])(implicit d: EntityDecoder[A]): IO[A] =
    req.flatMap(fetchAs(_)(d))

  @deprecated("Use expect", "0.14")
  def prepAs[T](req: IO[Request])(implicit d: EntityDecoder[T]): IO[T] =
    fetchAs(req)(d)

  /** Shuts this client down, and blocks until complete. */
  def shutdownNow(): Unit = shutdown.unsafeRunSync()

}

object Client {
  /** Creates a client from the specified service.  Useful for generating
    * pre-determined responses for requests in testing.
    *
    * @param service the service to respond to requests to this client
    */
  def fromHttpService(service: HttpService): Client = {
    val isShutdown = new AtomicBoolean(false)

    def interruptible(body: EntityBody, disposed: AtomicBoolean): Stream[IO, Byte]  = {
      def killable[F[_]](reason: String, killed: AtomicBoolean): Pipe[F, Byte, Byte] = {
        def go(killed: AtomicBoolean): Handle[F, Byte] => Pull[F, Byte, Unit] = {
          _.receiveOption{
            case Some((chunk, h)) =>
              if (killed.get){
                Pull.outputs[F, Byte](Stream.fail[F](new IOException(reason)))
              } else {
                Pull.output[F, Byte](chunk.toBytes) >> go(killed)(h)
              }
            case None => Pull.done
          }
        }

        _.pull(go(killed))
      }
      body
        .through(killable("response was disposed", disposed))
        .through(killable("client was shut down", isShutdown))
    }

    def disposableService(service: HttpService): Service[Request, DisposableResponse] =
      Service.lift { req: Request =>
        val disposed = new AtomicBoolean(false)
        val req0 = req.copy(body = interruptible(req.body, disposed))
        service(req0) map { maybeResp =>
          val resp = maybeResp.orNotFound
          DisposableResponse(
            resp.copy(body = interruptible(resp.body, disposed)),
            IO.delay(disposed.set(true))
          )
        }
      }

    Client(disposableService(service),
      IO.delay(isShutdown.set(true)))
  }
}

final case class UnexpectedStatus(status: Status) extends RuntimeException with NoStackTrace {
  override def getMessage: String = s"unexpected HTTP status: $status"
}
