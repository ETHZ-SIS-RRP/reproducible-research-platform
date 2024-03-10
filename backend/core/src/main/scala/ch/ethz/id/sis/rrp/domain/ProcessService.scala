package ch.ethz.id.sis.rrp
package domain

trait ProcessService:

  type Spec[A]

  type ProjectName = String
  type LogFileName = String

  def startServer(session: SessionInfo, project: Project): Operation[Spec[Unit]]

  def archiveProject(
      session: SessionInfo,
      project: Project,
      entityId: String,
      datasetType: String,
      properties: Map[String, String]
  ): Operation[Spec[LogFileName]]

  def archiveResults(
      session: SessionInfo,
      project: Project,
      entityId: String,
      datasetType: String,
      properties: Map[String, String]
  ): Operation[Spec[LogFileName]]

  def buildProjectFromGit(project: Project): Operation[Spec[Image]]

  def buildProjectFromArchive(
      sessionInfo: SessionInfo,
      project: Project,
      datasetId: String
  ): Operation[Spec[(ProjectName, Image)]]

  def createOfflineBundle(sessionInfo: SessionInfo, project: Project, description: String): Operation[Spec[LogFileName]]

  def exportImage(sessionInfo: SessionInfo, project: Project): Operation[Spec[LogFileName]]

  def execute[A](spec: Spec[A]): Operation[A]

  def getProcessesOf(project: Project): Operation[Set[ProcessInfo]]

  def stop(process: ProcessId): Operation[Unit]
