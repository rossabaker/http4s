package com.example.http4s
package jetty

import javax.servlet._

import com.codahale.metrics.MetricRegistry
import org.http4s.server.ServerApp
import org.http4s.server.jetty.JettyBuilder
import org.http4s.server.metrics._

object JettyExample extends ServerApp {
  val metrics = new MetricRegistry

  def server(args: List[String]) = JettyBuilder
    .bindHttp(8080)
    .mountService(ExampleService.service, "/http4s")
    .mountService(metricsService(metrics), "/metrics/*")
    .mountServlet({
      import javax.servlet._
      import javax.servlet.http._
      new HttpServlet {
        override def doGet(req: HttpServletRequest, resp: HttpServletResponse) = {
          resp.getWriter.println("pong")
          resp.getWriter.close();
        }
      }
    }, "/jetty/*")
    .mountFilter(NoneShallPass, "/http4s/science/black-knight/*")
    .start
}
