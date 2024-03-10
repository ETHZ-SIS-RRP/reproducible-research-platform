package ch.ethz.id.sis.rrp
package domain

import java.time.LocalDateTime
import java.io.Closeable
import java.io.File

trait ProjectService:

  def createNewProject(user: String, id: Option[String]): Operation[Project]

  def getProjectsOf(user: String): Operation[Set[Project]]

  def getNameOf(project: Project): Operation[String]
  def getMemoryRequestOf(project: Project): Operation[Double]
  def getCpuRequestOf(project: Project): Operation[Double]

  def getCreatedTimeOf(project: Project): Operation[LocalDateTime]
  def getSourceOf(project: Project): Operation[ProjectSource]
  def getStatusOf(project: Project): Operation[ProjectStatus]
  def getImageOf(project: Project): Operation[Image]

  def getResultsDirOf(project: Project): Operation[File]
  def getCloneDirOf(project: Project): Operation[File]
  def getLinksDirOf(project: Project): Operation[File]
  def getRunDirOf(project: Project): Operation[File]
  def getOfflineBundleDirOf(project: Project): Operation[File]

  def setName(project: Project, name: String): Operation[Unit]
  def setMemoryRequest(project: Project, value: Double): Operation[Boolean]
  def setCpuRequest(project: Project, value: Double): Operation[Boolean]
  def setSource(project: Project, source: ProjectSource): Operation[Unit]
  def setImage(project: Project, image: Image): Operation[Unit]
  def delete(project: Project): Operation[Unit]

  def initializeProjectFrom(share: Share, target: Project): Operation[Unit]
