package com.github.http4s
package test
package specs2

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

class Http4sSpecSpec extends Http4sSpec { def is =
  "get / should"                               ^
    "return 'Hello, world.'"                   ! e1

  // scalatra-specs2 does not depend on Http4s, so we'll create our own
  // simple servlet for a sanity check
  addServlet(new HttpServlet {
    override def doGet(req: HttpServletRequest, res: HttpServletResponse) {
      res.getWriter.write("Hello, world.");
    }
  }, "/*")

  def e1 = get("/") {
    body must_== "Hello, world."
  }
}
