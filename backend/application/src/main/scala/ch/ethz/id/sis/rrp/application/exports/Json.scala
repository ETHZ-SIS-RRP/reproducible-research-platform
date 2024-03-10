package ch.ethz.id.sis.rrp.application.exports

import ch.ethz.id.sis.rrp.json.given
import io.circe.Encoder
import io.circe.generic.semiauto._

case class ExportStatus(
    image: String,
    command: String,
    remote: String,
    commit: String,
    dataSetExports: Map[String, (String, String)]
)
given Encoder[ExportStatus] = deriveEncoder
