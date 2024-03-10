package ch.ethz.id.sis.rrp
package application

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.ethz.id.sis.rrp.application.authentication.Authenticate
import ch.ethz.id.sis.rrp.application.authentication.CheckSession
import ch.ethz.id.sis.rrp.application.authentication.EndSession
import ch.ethz.id.sis.rrp.application.bundle.CreateBundle
import ch.ethz.id.sis.rrp.application.bundle.DeleteBundle
import ch.ethz.id.sis.rrp.application.bundle.DownloadBundle
import ch.ethz.id.sis.rrp.application.bundle.ListBundles
import ch.ethz.id.sis.rrp.application.clone.CreateClone
import ch.ethz.id.sis.rrp.application.container.CommandContainer
import ch.ethz.id.sis.rrp.application.dataset.CreateDataSet
import ch.ethz.id.sis.rrp.application.dataset.ReadDataSetTypes
import ch.ethz.id.sis.rrp.application.event.ProjectEventStream
import ch.ethz.id.sis.rrp.application.exports.ExportImage
import ch.ethz.id.sis.rrp.application.exports.ReadExportStatus
import ch.ethz.id.sis.rrp.application.logs.ListLogs
import ch.ethz.id.sis.rrp.application.logs.ReadLog
import ch.ethz.id.sis.rrp.application.logs.TailLog
import ch.ethz.id.sis.rrp.application.mount.ReadMountFile
import ch.ethz.id.sis.rrp.application.mount.UpdateMountFile
import ch.ethz.id.sis.rrp.application.project.CreateProject
import ch.ethz.id.sis.rrp.application.project.ModifyProject
import ch.ethz.id.sis.rrp.application.project.CreateShare
import ch.ethz.id.sis.rrp.application.project.DeleteProject
import ch.ethz.id.sis.rrp.application.project.DeleteShare
import ch.ethz.id.sis.rrp.application.project.GetShare
import ch.ethz.id.sis.rrp.application.project.ListShares
import ch.ethz.id.sis.rrp.application.project.ReadFiles
import ch.ethz.id.sis.rrp.domain.ArchiveService
import ch.ethz.id.sis.rrp.domain.archive.OpenbisArchiveServiceInterpreter
import ch.ethz.id.sis.rrp.domain.authentication.BasicAuthenticationServiceInterpreter
import ch.ethz.id.sis.rrp.domain.authentication.PersonalAccessTokenAuthenticationServiceInterpreter
import ch.ethz.id.sis.rrp.domain.bundle.BundleServiceInterpreter
import ch.ethz.id.sis.rrp.domain.clone.JGitCloneServiceInterpreter
import ch.ethz.id.sis.rrp.domain.log.FileSystemLogServiceInterpreter
import ch.ethz.id.sis.rrp.domain.mount.OpenbisMountingServiceInterpreter
import ch.ethz.id.sis.rrp.domain.process.KubernetesProcessServiceInterpreter
import ch.ethz.id.sis.rrp.domain.project.ProjectServiceInterpreter
import ch.ethz.id.sis.rrp.domain.share.ShareServiceInterpreter
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpService
import ch.ethz.id.sis.rrp.http._
import ch.ethz.id.sis.rrp.http.cookie.CryptoCookieJar
import ch.ethz.id.sis.rrp.infra.bundle.FileSystemBundleRepository
import ch.ethz.id.sis.rrp.infra.metadata.FileSystemMetadataRepository
import ch.ethz.id.sis.rrp.infra.share.FileSystemShareRepository
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi
import ch.systemsx.cisd.common.spring.HttpInvokerUtils

import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.net.URL
import java.util.Properties
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.Using

import EnvironmentVariableReader._

class HttpServiceBuilder(args: Array[String]):
  given envVars: EnvironmentVariableReader = new EnvironmentVariableReader()

  def build(): HttpService =
    val port                        = "rrp_backend_port" as integer
    val openbisURL                  = "rrp_openbis_url" as url
    val dssURL                      = "rrp_dss_url" as url
    val sessionSecret               = "rrp_cookie_encryption_secret" as file
    val storageRoot                 = "rrp_storage_root" as file
    val sessionCookieName           = "rrp_session_cookie_name" as such
    val envNamespace                = "rrp_namespace" as such
    val imageTag                    = "rrp_image_tag" as such
    val hostname                    = "rrp_hostname" as such
    val envExternalRegistry         = "rrp_external_registry" as such
    val envExportRegistry           = "rrp_export_registry" as such
    val envSshHost                  = "rrp_ssh_host" as such
    val envSshPort                  = "rrp_ssh_port" as integer
    val envServerLocation           = "rrp_analysis_server_location" as url
    val personalAccessTokensEnabled = "rrp_personal_access_tokens" as boolean
    val testResearchCollection      = "rrp_use_test_research_collection" as boolean
    val envDefaultMemoryRequest     = "rrp_default_memory_request" as real
    val envDefaultCpuRequest        = "rrp_default_cpu_request" as real
    val envMaxMemoryRequest         = "rrp_max_memory_request" as real
    val envMaxCpuRequest            = "rrp_max_cpu_request" as real

    val openbisTimeout = 15000
    envVars.confReport()

    given actorSystem: ActorSystem = ActorSystem()
    given ExecutionContextExecutor = actorSystem.dispatcher

    val openbisClient = HttpInvokerUtils
      .createServiceStub(
        classOf[IApplicationServerApi],
        openbisURL.toString() + IApplicationServerApi.SERVICE_URL,
        openbisTimeout
      )

    val dssClient = HttpInvokerUtils.createStreamSupportingServiceStub(
      classOf[IDataStoreServerApi],
      dssURL.toString() + IDataStoreServerApi.SERVICE_URL,
      openbisTimeout
    )

    val homeRoot = new File(storageRoot, "home")

    def homeDirOf(user: String): File = new File(homeRoot, user)

    def storageDir(path: Project => String): Project => File =
      project => {
        val dir = new File(homeRoot, path(project))
        dir.mkdirs()
        dir
      }

    val shareRepository = new FileSystemShareRepository(
      storageDir(project => s"${project.user}/projects/${project.id}/shares")
    )

    val metadataRepository = new FileSystemMetadataRepository(
      storageDir(project => s"${project.user}/projects/${project.id}/metadata")
    )

    val logDir = storageDir(project => s"${project.user}/projects/${project.id}/logs")

    val bundleRepository = new FileSystemBundleRepository(
      storageDir(project => s"${project.user}/projects/${project.id}/offline"),
      logDir
    )

    def subpath(dir: File): String =
      return dir.getAbsolutePath().substring(storageRoot.getAbsolutePath().length() + 1)

    val properties = new Properties()
    properties.load(Source.fromResource("build-info.properties").bufferedReader())

    val debugDir = new File(storageRoot, "debug")
    debugDir.mkdirs()
    val debugId = java.util.UUID.randomUUID().toString()

    val env = new ExecutionEnvironment {
      override val namespace        = envNamespace
      override val tag              = imageTag
      override val archiveSystem    = OpenBis
      override val internalRegistry = hostname
      override val externalRegistry = envExternalRegistry
      override val exportRegistry   = envExportRegistry
      override val openbis          = openbisURL
      override val dss              = dssURL
      override val sshHost          = envSshHost
      override val sshPort          = envSshPort
      override val serverLocation   = envServerLocation
      override val archiveService   = OpenbisArchiveServiceInterpreter(openbisClient, dssClient)
      override val authenticationService =
        if (personalAccessTokensEnabled)
          PersonalAccessTokenAuthenticationServiceInterpreter(openbisClient)
        else
          BasicAuthenticationServiceInterpreter(openbisClient)
      override val bundleService              = BundleServiceInterpreter(bundleRepository)
      override val cloneService               = JGitCloneServiceInterpreter(actorSystem, logDir)
      override val projectService             = ProjectServiceInterpreter(homeDirOf, metadataRepository, logDir)
      override val mountingService            = OpenbisMountingServiceInterpreter(openbisClient, testResearchCollection)
      override val shareService               = ShareServiceInterpreter(shareRepository)
      override val logService                 = FileSystemLogServiceInterpreter(logDir)
      override val processService             = KubernetesProcessServiceInterpreter(namespace, subpath, logDir)
      override val buildInfo                  = properties
      override val serverCommandConfiguration = PropertyFileServerCommandConfiguration("launch.properties")
      override val debugWriter                = new PrintWriter(new File(debugDir, debugId))
      override val defaultMemoryRequest       = envDefaultMemoryRequest min envMaxMemoryRequest
      override val defaultCpuRequest          = envDefaultCpuRequest min envMaxCpuRequest
      override val maxMemoryRequest           = envMaxMemoryRequest
      override val maxCpuRequest              = envMaxCpuRequest
    }

    given AdapterConfig =
      AdapterConfig(sessionCookieName, CryptoCookieJar(Source.fromFile(sessionSecret).getLines().mkString), actorSystem)

    val archiveErrorHandling: PartialFunction[Error, Route] = {
      case OpenbisArchiveServiceInterpreter.OpenbisAuthenticationFailed =>
        setCookie(HttpCookie(sessionCookieName, value = "", path = Some("/"))) {
          complete(StatusCodes.Unauthorized)
        }
    }

    log(s"debug id: $debugId")

    HttpService(
      port,
      actorSystem,
      Authenticate(env),
      CheckSession(env),
      EndSession(env),
      CreateClone(env),
      ReadLog(env),
      TailLog(env),
      CreateProject(env),
      ModifyProject(env),
      DeleteProject(env),
      ReadFiles(env),
      ProjectEventStream(env),
      ReadDataSetTypes(env) onError archiveErrorHandling,
      CreateDataSet(env) onError archiveErrorHandling,
      CommandContainer(env),
      ReadMountFile(env),
      UpdateMountFile(env),
      CreateShare(env),
      ListShares(env),
      GetShare(env),
      DeleteShare(env),
      ListLogs(env),
      CreateBundle(env),
      ListBundles(env),
      DownloadBundle(env),
      DeleteBundle(env),
      ReadExportStatus(env),
      ExportImage(env)
    )
