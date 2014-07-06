package org.http4s.examples
package servlet

import org.http4s.jetty.JettyServer
import scala.concurrent.duration._

object JettyExample extends App {
  JettyServer.newBuilder
    .withAsyncTimeout(5.seconds)
    .mountService(ExampleService.service, "/http4s")
    .mountServlet(new RawServlet, "/raw/*")
    .run()
    .join()
}
