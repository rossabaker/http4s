package org.http4s
package server
package staticcontent

import cats.effect.IO

/** Cache strategy that doesn't cache anything, ever. */
object NoopCacheStrategy extends CacheStrategy {
  override def cache(uriPath: String, resp: Response): IO[Response] = IO.now(resp)
}
