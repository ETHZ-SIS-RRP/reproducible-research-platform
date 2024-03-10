package ch.ethz.id.sis.rrp
package infra

import java.time.LocalDateTime

trait ShareRepository:
  type ShareId = String
  case class PersistedShare(
      repository: String,
      commitHash: String,
      createdAt: LocalDateTime,
      description: String
  )
  def readProjectOf(shareId: String): Operation[Project]
  def createShare(project: Project, data: PersistedShare): Operation[String]
  def readShare(shareId: String): Operation[Option[PersistedShare]]
  def listShares(project: Project): Operation[List[(ShareId, PersistedShare)]]
  def deleteShare(project: Project, shareId: String): Operation[Unit]
