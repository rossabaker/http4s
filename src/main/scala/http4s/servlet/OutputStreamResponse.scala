package http4s.servlet

import java.io.OutputStream

import http4s.{ResponseStatus, Response}

package object servlet {
  implicit def toOutputStreamResponse(res: http4s.Response) =
    new OutputStreamResponse(res.status, res.headers)
}

class OutputStreamResponse(status: ResponseStatus, headers: Map[String, String]) {
  def withOutputStream(responseSerializer: OutputStream => Unit) =
    Response(status, responseSerializer, headers)
}

class Response(status: ResponseStatus, headers: Map[String, String]) extends http4s.Response[OutputStream](status, null, headers) {

  implicit def toOutputStreamResponse(res: http4s.Response) =
    new Response(res.status, headers)
  def withOutputStream(responseSerializer: OutputStream => Unit) =
    body = responseSerializer

}
