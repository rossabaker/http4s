package org.http4s
package server

import cats.data.Kleisli
import cats.effect._
import cats.implicits._

object Router {

  import middleware.URITranslation.{translateRoot => translate}

  /**
    * Defines an HttpPartial based on list of mappings.
    * @see define
    */
  def apply[F[_]: Sync](mappings: (String, HttpPartial[F])*): HttpPartial[F] =
    define(mappings: _*)(HttpPartial.empty[F])

  /**
    * Defines an HttpPartial based on list of mappings and
    * a default Service to be used when none in the list match incomming requests.
    *
    * The mappings are processed in descending order (longest first) of prefix length.
    */
  def define[F[_]: Sync](mappings: (String, HttpPartial[F])*)(
      default: HttpPartial[F]): HttpPartial[F] =
    mappings.sortBy(_._1.length).foldLeft(default) {
      case (acc, (prefix, service)) =>
        if (prefix.isEmpty || prefix == "/") service <+> acc
        else
          Kleisli { req =>
            (
              if (req.pathInfo.startsWith(prefix))
                translate(prefix)(service) <+> acc
              else
                acc
            )(req)
          }
    }

}
