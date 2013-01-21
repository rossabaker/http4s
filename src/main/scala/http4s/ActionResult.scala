package http4s

case class Response[T](status: ResponseStatus, var body: T, headers: Map[String, String])

private object Helpers {
  def responseStatus(status: Int, reason: String) = reason match {
    case "" | null => ResponseStatus(status)
    case _  => new ResponseStatus(status, reason) 
  }
}

import Helpers._

object Ok {
  def apply[T](body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(200, reason), body, headers)
}

object Created {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(201, reason), body, headers)
}

object Accepted {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(202, reason), body, headers)
}

object NonAuthoritativeInformation {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(203, reason), body, headers)
}

object NoContent {
  def apply(headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(204, reason), Unit, headers)
}

object ResetContent {
  def apply(headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(205, reason), Unit, headers)
}

object PartialContent {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(206, reason), body, headers)
}

object MultiStatus {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(207, reason), body, headers)
}

object AlreadyReported {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(208, reason), body, headers)
}

object IMUsed {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(226, reason), body, headers)
}

object MultipleChoices {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(300, reason), body, headers)
}

object MovedPermanently {
  def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") = 
    Response(responseStatus(301, reason), Unit, Map("Location" -> location) ++ headers)
}

object Found {
  def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") = 
    Response(responseStatus(302, reason), Unit, Map("Location" -> location) ++ headers)
}

object SeeOther {
  def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") = 
    Response(responseStatus(303, reason), Unit, Map("Location" -> location) ++ headers)
}

object NotModified {
  def apply(headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(304, reason), Unit, headers)
}

object UseProxy {
  def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") = 
    Response(responseStatus(305, reason), Unit, Map("Location" -> location) ++ headers)
}

object TemporaryRedirect {
  def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") = 
    Response(responseStatus(307, reason), Unit, Map("Location" -> location) ++ headers)
}

object PermanentRedirect {
  def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") = 
    Response(responseStatus(308, reason), Unit, Map("Location" -> location) ++ headers)
}

object BadRequest {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(400, reason), body, headers)
}

object Unauthorized {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(401, reason), body, headers)
}

object PaymentRequired {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(402, reason), body, headers)
}

object Forbidden {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(403, reason), body, headers)
}

object NotFound {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(404, reason), body, headers)
}

object MethodNotAllowed {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(405, reason), body, headers)
}

object NotAcceptable {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(406, reason), body, headers)
}

object ProxyAuthenticationRequired {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(407, reason), body, headers)
}

object RequestTimeout {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(408, reason), body, headers)
}

object Conflict {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(409, reason), body, headers)
}

object Gone {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(410, reason), body, headers)
}

object LengthRequired {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(411, reason), body, headers)
}

object PreconditionFailed {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(412, reason), body, headers)
}

object RequestEntityTooLarge {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(413, reason), body, headers)
}

object RequestURITooLong {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(414, reason), body, headers)
}

object UnsupportedMediaType {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(415, reason), body, headers)
}

object RequestedRangeNotSatisfiable {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(416, reason), body, headers)
}

object ExpectationFailed {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(417, reason), body, headers)
}

object UnprocessableEntity {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(422, reason), body, headers)
}

object Locked {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(423, reason), body, headers)
}

object FailedDependency {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(424, reason), body, headers)
}

object UpgradeRequired {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(426, reason), body, headers)
}

object PreconditionRequired {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(428, reason), body, headers)
}

object TooManyRequests {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(429, reason), body, headers)
}

object RequestHeaderFieldsTooLarge {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(431, reason), body, headers)
}

object InternalServerError {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(500, reason), body, headers)
}

object NotImplemented {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(501, reason), body, headers)
}

object BadGateway {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(502, reason), body, headers)
}

object ServiceUnavailable {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(503, reason), body, headers)
}

object GatewayTimeout {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(504, reason), body, headers)
}

object HTTPVersionNotSupported {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(505, reason), body, headers)
}

object VariantAlsoNegotiates {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(506, reason), body, headers)
}

object InsufficientStorage {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(507, reason), body, headers)
}

object LoopDetected {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(508, reason), body, headers)
}

object NotExtended {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(510, reason), body, headers)
}

object NetworkAuthenticationRequired {
  def apply(body: T = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    Response(responseStatus(511, reason), body, headers)
}

