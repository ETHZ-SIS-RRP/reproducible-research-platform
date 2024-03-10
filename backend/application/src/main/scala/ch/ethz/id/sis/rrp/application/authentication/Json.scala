package ch.ethz.id.sis.rrp.application
package authentication

import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto._
import cats.syntax.functor._

sealed trait LoginRequest
case class CredentialsLoginRequest(username: String, password: String) extends LoginRequest
case class PersonalAccessTokenLoginRequest(token: String)              extends LoginRequest

given Decoder[CredentialsLoginRequest]         = deriveDecoder
given Decoder[PersonalAccessTokenLoginRequest] = deriveDecoder

given Decoder[LoginRequest] =
  List[Decoder[LoginRequest]](
    Decoder[PersonalAccessTokenLoginRequest].widen,
    Decoder[CredentialsLoginRequest].widen
  ).reduceLeft(_ or _)

case class SessionCheckResponse(
    username: String,
    validTo: String,
    updates: List[ProjectUpdateEvent]
)
given Encoder[SessionCheckResponse] = deriveEncoder
