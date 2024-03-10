package ch.ethz.id.sis.rrp
package application
package dataset

import ch.ethz.id.sis.rrp.json.given
import io.circe.Decoder
import io.circe.generic.semiauto._

import java.time.LocalDateTime

case class CreateDataSetRequest(
    dataSetType: String,
    properties: Map[String, String],
    entityId: String,
    projectArchive: Boolean
)

given Decoder[CreateDataSetRequest] = deriveDecoder
