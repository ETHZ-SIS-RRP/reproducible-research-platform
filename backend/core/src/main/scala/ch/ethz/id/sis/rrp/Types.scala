package ch.ethz.id.sis.rrp

import java.time.LocalDateTime
import java.time.LocalDate

trait Error:
  val message: String
object Error:
  def apply(s: String) = new Error { override val message = s }

case class Credentials(username: String, password: String)
case class ArchiveType(name: String, propertyAssignments: List[PropertyAssignment])
case class LogFileContent(lines: List[String], error: Option[String], hasMore: Boolean)
case class LogLine(number: Long, data: String, isPartial: Boolean = false, end: Option[String] = None)

case class NewLogLine(data: String, end: Option[String] = None)
case class PropertyAssignment(name: String, dataType: String, mandatory: Boolean)

case class RemoteTracking(val branch: String, val ahead: Int, val behind: Int):
  override def toString(): String =
    if (ahead > 0 & behind > 0) {
      s"Diverged from $branch ($ahead commits ahead, $behind commits behind)"
    } else if (ahead > 0) {
      s"$ahead commits ahead of $branch"
    } else if (behind > 0) {
      s"$behind commits behind of $branch"
    } else {
      s"Up-to-date with $branch"
    }

case class CommitDetails(
    val hash: String,
    val message: String,
    val timestamp: LocalDateTime,
    val authorName: String,
    val authorEmail: String
)

case class CloneDetails(
    val remote: Option[String],
    val clean: Boolean,
    val branch: String,
    val commit: CommitDetails,
    val remoteTracking: Option[RemoteTracking]
)

sealed trait ProjectSource
case class GitSource(remote: String, commitHash: String) extends ProjectSource
case class ShareIdSource(share: Share)                   extends ProjectSource
case class ArchiveSource(archiveId: String)              extends ProjectSource
case object SourceNotYetDefined                          extends ProjectSource

sealed trait ProjectStatus:
  val cloneComplete: Boolean

case object Initializing extends ProjectStatus:
  val cloneComplete = false
case object Cloning extends ProjectStatus:
  val cloneComplete = false
case class CloneFailed(reason: String) extends ProjectStatus:
  override def toString() = "CloneFailed"
  val cloneComplete       = false
case object Cloned extends ProjectStatus:
  val cloneComplete = true
case object Building extends ProjectStatus:
  val cloneComplete = true
case object BuildFailed extends ProjectStatus:
  val cloneComplete = true
case object BuildingFromArchive extends ProjectStatus:
  val cloneComplete = false
case object BuildFromArchiveFailed extends ProjectStatus:
  val cloneComplete = false
case object Ready extends ProjectStatus:
  val cloneComplete = true

case class Project(user: String, id: String):
  override def toString = s"$user/$id"

case class SessionInfo(username: String, openbisToken: String, validTo: LocalDateTime)

sealed trait ContainerStatus
case object ContainerRunning    extends ContainerStatus
case object ContainerProcessing extends ContainerStatus
case object ContainerNotRunning extends ContainerStatus

case class Container(project: Project, ready: Boolean, build: Boolean)

case class Share(
    id: String,
    author: String,
    projectId: String,
    projectName: String,
    repository: String,
    createdAt: LocalDateTime,
    description: String,
    commit: CommitDetails,
    error: Option[String]
)

enum LogType:
  case Clone, Build, ArchiveProject, ArchiveResults, UnarchiveProject, CreateOfflineBundle, Server, ImageExport

case class LogEntry(id: String, logType: LogType, timestamp: LocalDateTime, outcome: Option[Either[String, Unit]])

sealed trait ArchiveSystem
case object OpenBis extends ArchiveSystem

sealed trait ProcessType
case object Archive              extends ProcessType
case object Unarchive            extends ProcessType
case object Build                extends ProcessType
case object OfflineBundleProcess extends ProcessType
case object Server               extends ProcessType

type ProcessId = String
case class ProcessInfo(processType: ProcessType, isRunning: Boolean, processId: ProcessId)

case class Image(
    plainName: String,
    buildToolName: String,
    buildToolVersion: String
)

case class OfflineBundle(
    project: Project,
    name: String,
    description: String,
    createdAt: LocalDateTime,
    commit: CommitDetails
)
case class BundleStatus(successful: Boolean, finished: Boolean, size: Long)
