package org.http4s.client

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.http4s.client.Client.BadResponse
import org.http4s._

import scala.util.control.NoStackTrace
import scalaz.concurrent.Task


trait Client extends LazyLogging {

  /** Prepare a single request
    * @param req [[Request]] containing the headers, URI, etc.
    * @return Task which will generate the Response
    */
  def prepare(req: Request): Task[Response]

  /** Prepare a single request
    * @param req [[Request]] containing the headers, URI, etc.
    * @return Task which will generate the Response
    */
  final def prepare(req: Task[Request]): Task[Response] = req.flatMap(prepare(_))

  /** Shutdown this client, closing any open connections and freeing resources */
  def shutdown(): Task[Unit]

  final def request[A](req: Task[Request], decoder: EntityDecoder[A]): Task[A] =
    req.flatMap(req => request(req, decoder))
  
  final def request[A](req: Request, decoder: EntityDecoder[A]): Task[A] = prepare(req).flatMap { resp =>
    if (resp.status == Status.Ok) {
      if (!decoder.matchesMediaType(resp)) {
        val tpe = resp.contentType.getOrElse("Indefined")
        logger.warn(s"Response Content-Type, '$tpe', doesn't match the types " +
                    s"supported by the decoder: ${decoder.consumes}")
      }
      decoder(resp)
    }
    else EntityBody.text(resp).flatMap(str => Task.fail(BadResponse(resp.status, str)))
  }
}

object Client {

  case class BadResponse(status: Status, msg: String) extends Exception with NoStackTrace {
    override def getMessage: String = s"Bad Response, $status: '$msg'"
  }
}
