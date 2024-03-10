package ch.ethz.id.sis.rrp.application
package clone

import io.circe.Decoder
import io.circe.generic.semiauto._

case class GitCloneRequest(
    repository: String,
    projectId: String,
    username: Option[String],
    password: Option[String],
    shareId: Option[String]
)
given Decoder[GitCloneRequest] = deriveDecoder
