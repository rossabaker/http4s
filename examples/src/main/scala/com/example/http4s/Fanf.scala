package com.example

import org.http4s._, dsl._, util._
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration._
import scala.util.Random

object fanf {
    //for debuging - of course works correctly only if sequential
    val counterError   = new AtomicInteger(0)
    val counterSuccess = new AtomicInteger(0)

  def service = HttpService {
      case _ -> Root =>
        MethodNotAllowed()

      case GET -> Root / "single_node1" =>
        Ok{
          counterSuccess.getAndIncrement()
          "books"
        }

      case GET -> Root / x =>
        Ok {
          counterSuccess.getAndIncrement()
          x.toString
        }

      case r @ GET -> Root / "delay" / x =>
        r.headers.get(CaseInsensitiveString("nodeId")).map( _.value) match {
          case Some(`x`) =>
            Ok {
              counterSuccess.getAndIncrement()
              x.toString
            }.after(Random.nextInt(1000).millis)

          case _ =>
            Forbidden {
              counterError.getAndIncrement()
              "node id was not found in the 'nodeid' header"
            }
        }

      case r @ POST -> Root / "delay" =>

        val headerId = r.headers.get(CaseInsensitiveString("nodeId")).map( _.value)

        r.decode[UrlForm] { data =>
          val formId = data.values.get("nodeId").flatMap(_.headOption)

          (headerId, formId) match {
            case (Some(x), Some(y)) if x == y =>
              Ok {
                counterSuccess.getAndIncrement()
                "plop"
              }.after(Random.nextInt(1000).millis)

            case _ =>
              Forbidden {
                counterError.getAndIncrement()
                "node id was not found in post form (key=nodeId)"
              }
          }
        }

      case GET -> Root / "faileven" / x =>
        // x === "nodeXX" or root
        if(x != "root" && x.replaceAll("node", "").toInt % 2 == 0) {
          Forbidden {
            counterError.getAndIncrement()
            "Not authorized"
          }
        } else {
          Ok {
            counterSuccess.getAndIncrement()
            x.toString
          }
        }
  }
}
