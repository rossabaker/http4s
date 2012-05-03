package com.github.http4s
package test
package specs2

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

class MutableHttp4sSpecSpec extends MutableHttp4sSpec {
  // scalatra-specs2 does not depend on Http4s, so we'll create our own
  // simple servlet for a sanity check
  addServlet(new HttpServlet {
    override def doGet(req: HttpServletRequest, res: HttpServletResponse) {
      res.getWriter.write("Hello, world.");
    }
  }, "/*")

  "get" should {
    "be able to verify the response body" in {
      get("/") {
        body must_== "Hello, world."
      }
    }
  }
}
