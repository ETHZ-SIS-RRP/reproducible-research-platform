package ch.ethz.id.sis.rrp.application.bundle

import ch.ethz.id.sis.rrp.json.given
import io.circe.Encoder
import io.circe.generic.semiauto._

import java.time.LocalDateTime

case class BundleInfo(
    description: String,
    createdAt: LocalDateTime,
    success: Boolean,
    finished: Boolean,
    fileSize: Long,
    commitHash: String,
    commitMessage: String,
    link: String
)
given Encoder[BundleInfo] = deriveEncoder
