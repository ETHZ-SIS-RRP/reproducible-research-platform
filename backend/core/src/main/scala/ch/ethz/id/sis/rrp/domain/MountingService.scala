package ch.ethz.id.sis.rrp
package domain

import java.io.File

trait MountingService:

  type DatasetId = String
  type MountPath = String
  type ExportURL = String

  def readMountConfigOf(project: Project): Operation[String]
  def refreshMountsOf(project: Project, session: SessionInfo): Operation[Unit]
  def updateMountConfigOf(project: Project, spec: String, session: SessionInfo): Operation[Unit]
  def readExportStatusOf(
      project: Project,
      sessionInfo: SessionInfo
  ): Operation[Map[MountPath, (DatasetId, Option[DatasetId])]]
