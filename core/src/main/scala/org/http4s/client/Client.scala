package org.http4s
package client

import scalaz.concurrent.Task

/**
 * Created by Bryce Anderson on 6/24/14.
 */


trait Client {
  final def request(req: Request): Task[Response] = request(req::Nil).map{ resp =>
    if (resp.isEmpty) sys.error(s"Didn't receive a response for request $req")
    else resp.head
  }

  def request(urls: Seq[Request]): Task[Seq[Response]]
}
