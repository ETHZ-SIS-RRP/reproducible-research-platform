package ch.ethz.id.sis.rrp
package domain

import java.io.File

trait ArchiveService:

  type ArchiveId = String
  def archiveResults(
      project: Project,
      archiveTypeName: String,
      archiveName: String,
      properties: Map[String, String],
      session: SessionInfo
  ): Operation[ArchiveId]

  def archiveProject(
      project: Project,
      archiveTypeName: String,
      archiveName: String,
      properties: Map[String, String],
      session: SessionInfo
  ): Operation[ArchiveId]

  def listArchiveTypes(session: SessionInfo): Operation[List[ArchiveType]]

  def getArchiveProperties(session: SessionInfo, archiveId: String): Operation[Map[String, String]]

  def readFileFromArchive(session: SessionInfo, archiveId: String, path: String): Operation[Option[String]]
