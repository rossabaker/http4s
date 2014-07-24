package org.http4s.client

import org.http4s.{Request, Response}

import scalaz.concurrent.Task


trait Client {

  /** Make a single request
    * @param req [[Request]] containing the headers, URI, etc.
    * @return Task which will generate the Response
    */
  def request(req: Request): Task[Response]

  /** Make a single request
    * @param req [[Request]] containing the headers, URI, etc.
    * @return Task which will generate the Response
    */
  final def prepare(req: Task[Request]): Task[Response] = req.flatMap(request(_))

  /** Shutdown this client, closing any open connections and freeing resources */
  def shutdown(): Task[Unit]
}
