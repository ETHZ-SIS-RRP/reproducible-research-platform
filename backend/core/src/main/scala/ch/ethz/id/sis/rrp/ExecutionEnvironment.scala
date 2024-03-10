package ch.ethz.id.sis.rrp

import ch.ethz.id.sis.rrp.domain.ArchiveService
import ch.ethz.id.sis.rrp.domain.AuthenticationService
import ch.ethz.id.sis.rrp.domain.BundleService
import ch.ethz.id.sis.rrp.domain.CloneService
import ch.ethz.id.sis.rrp.domain.MountingService
import ch.ethz.id.sis.rrp.domain.ProcessService
import ch.ethz.id.sis.rrp.domain.ProjectService
import ch.ethz.id.sis.rrp.domain.ShareService

import java.io.File
import java.net.URL
import java.util.Properties
import java.io.PrintWriter

trait ExecutionEnvironment:
  val namespace: String
  val tag: String
  val internalRegistry: String
  val externalRegistry: String
  val exportRegistry: String
  val authenticationService: AuthenticationService
  val archiveSystem: ArchiveSystem
  val archiveService: ArchiveService
  val bundleService: BundleService
  val cloneService: CloneService
  val projectService: ProjectService
  val processService: ProcessService
  val shareService: ShareService
  val mountingService: MountingService
  val logService: LogService
  val openbis: URL
  val dss: URL
  val sshHost: String
  val sshPort: Int
  val serverLocation: URL
  val buildInfo: Properties
  val serverCommandConfiguration: ServerCommandConfiguration
  val debugWriter: PrintWriter
  val defaultMemoryRequest: Double
  val defaultCpuRequest: Double
  val maxMemoryRequest: Double
  val maxCpuRequest: Double
