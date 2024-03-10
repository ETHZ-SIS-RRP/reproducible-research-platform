package ch.ethz.id.sis.rrp
package json

import io.circe.Codec
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.semiauto._
import io.circe.syntax._

import java.time.LocalDateTime
import java.io.File

given Encoder[LocalDateTime] with
  override def apply(date: LocalDateTime): io.circe.Json =
    formatter.format(date).asJson

given Decoder[LocalDateTime] = Decoder.decodeString.map(LocalDateTime.parse(_, formatter))

given Encoder[File] with
  override def apply(file: File): io.circe.Json =
    file.getAbsolutePath.asJson

given Decoder[File] = Decoder.decodeString.map(new File(_))

given Codec[SessionInfo] = deriveCodec

given Encoder[CommitDetails]  = deriveEncoder
given Encoder[RemoteTracking] = deriveEncoder
given Encoder[CloneDetails]   = deriveEncoder

given Encoder[PropertyAssignment] = deriveEncoder
given Encoder[ArchiveType]        = deriveEncoder

given Encoder[Share] = deriveEncoder

/*
given Encoder[GitSource]                = deriveEncoder
given Encoder[ShareIdSource]            = deriveEncoder
given Encoder[ArchiveSource.type]       = deriveEncoder
given Encoder[SourceNotYetDefined.type] = deriveEncoder

given Encoder[ProjectSource] = Encoder.instance {
  case git: GitSource       => git.asJson
  case share: ShareIdSource => share.asJson
  case ArchiveSource        => ArchiveSource.asJson
  case SourceNotYetDefined  => SourceNotYetDefined.asJson
}*/
given Codec[ProjectSource]   = deriveCodec
given Codec[Image]           = deriveCodec
given Encoder[LogEntry]      = deriveEncoder
given Encoder[OfflineBundle] = deriveEncoder

given toJson[A](using enc: Encoder[A]): Conversion[A, Json] with
  def apply(a: A): Json = enc(a)
