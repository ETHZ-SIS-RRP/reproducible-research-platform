package ch.ethz.id.sis.rrp
package application
package logs

import ch.ethz.id.sis.rrp.json.given
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.syntax._

case class LogEntryReference(
    name: String,
    url: String,
    `type`: String,
    timestamp: String,
    successful: Option[Boolean],
    message: String
)
given Encoder[LogEntryReference] = deriveEncoder
