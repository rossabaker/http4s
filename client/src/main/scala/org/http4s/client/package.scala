package org.http4s

import scalaz.concurrent.Task

package object client {
  implicit class ClientSyntax(request: Task[Request]) {
    def exec(implicit client: Client): Task[Response] = client.prepare(request)
  }
}
