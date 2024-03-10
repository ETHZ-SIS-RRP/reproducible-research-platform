package ch.ethz.id.sis.rrp
package application
package project

import cats.syntax.functor._
import ch.ethz.id.sis.rrp.json.given
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.syntax._

import java.time.LocalDateTime

sealed trait CreateProjectRequest {
  val projectId: String
}

case class CreateProjectFromCloneRequest(
    name: String,
    projectId: String,
    branch: String
) extends CreateProjectRequest

case class CreateProjectFromArchiveRequest(
    projectId: String,
    archiveId: String
) extends CreateProjectRequest

given Decoder[CreateProjectFromCloneRequest]   = deriveDecoder
given Decoder[CreateProjectFromArchiveRequest] = deriveDecoder

given Decoder[CreateProjectRequest] =
  List[Decoder[CreateProjectRequest]](
    Decoder[CreateProjectFromCloneRequest].widen,
    Decoder[CreateProjectFromArchiveRequest].widen
  ).reduceLeft(_ or _)

sealed trait FileSystemNode {
  val name: String
  val modificationTime: LocalDateTime
}

case class FileNode(override val name: String, override val modificationTime: LocalDateTime, size: Long)
    extends FileSystemNode

case class DirectoryNode(
    override val name: String,
    override val modificationTime: LocalDateTime,
    content: List[FileSystemNode]
) extends FileSystemNode

given Encoder[FileSystemNode] = Encoder.instance {
  case file: FileNode     => file.asJson
  case dir: DirectoryNode => dir.asJson
}

given Encoder[FileNode]      = deriveEncoder
given Encoder[DirectoryNode] = deriveEncoder

case class ModifyProjectRequest(key: String, value: String)
given Decoder[ModifyProjectRequest] = deriveDecoder
