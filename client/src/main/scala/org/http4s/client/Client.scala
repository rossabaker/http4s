package org.http4s.client

import org.http4s.client.Client.BadResponse
import org.http4s._

import scala.util.control.NoStackTrace
import scalaz.concurrent.Task

trait EntityDecoder[T] {
  def apply(msg: Message, limit: Int): Task[T]
  final def apply(msg: Message): Task[T] = apply(msg, EntityBody.DefaultMaxEntitySize)
}

object EntityDecoder {
  implicit val text: EntityDecoder[String] = new EntityDecoder[String] {
    override def apply(msg: Message, limit: Int): Task[String] = EntityBody.text(msg, limit)
  }
}


trait Client {

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

  final def request[A](req: Task[Request])(implicit parser: EntityDecoder[A]): Task[A] =
    req.flatMap(req => request(req)(parser))
  
  final def request[A](req: Request)(implicit parser: EntityDecoder[A]): Task[A] = prepare(req).flatMap { resp =>
    if (resp.status == Status.Ok) parser(resp)
    else EntityBody.text(resp).flatMap(str => Task.fail(BadResponse(resp.status, str)))
  }
}

object Client {
  case class BadResponse(status: Status, msg: String) extends Exception with NoStackTrace {
    override def getMessage: String = s"Bad Response, $status: '$msg'"
  }
}
