package org.http4s
package client

import scalaz.concurrent.Task

/**
 * Created by Bryce Anderson on 6/24/14.
 */


trait Client {

  /** Make a single request
    * @param req [[Request]] containing the headers, URI, etc.
    * @return Task which will generate the Response
    */
  def request(req: Request): Task[Response]

  /** Shutdown this client, closing any open connections and freeing resources */
  def shutdown(): Task[Unit]
}
