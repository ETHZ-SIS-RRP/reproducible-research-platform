package ch.ethz.id.sis.rrp
package application
package container

import ch.ethz.id.sis.rrp.json.given
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.syntax._

case class ProjectCommand(op: String)
given Decoder[ProjectCommand] = deriveDecoder
