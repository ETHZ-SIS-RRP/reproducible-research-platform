package ch.ethz.id.sis.rrp
package domain
package project

import ch.ethz.id.sis.rrp.json.given
import io.circe.Codec
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.CharSet

import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Base64
import java.util.UUID
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Using
import java.io.Closeable
import ch.ethz.id.sis.rrp.infra.MetadataRepository

case class ProjectServiceInterpreter(
    homeDirOf: String => File,
    metadataRepository: MetadataRepository,
    getLogDir: Project => File
) extends ProjectService:

  private def dirOf(project: Project): Operation[File] = unit(
    new File(homeDirOf(project.user), s"projects/${project.id}")
  )

  private def dirOf(project: Project, subpath: String): Operation[File] =
    for
      dir <- dirOf(project).map(new File(_, subpath))
      _   <- sideEffect { dir.mkdirs() }
    yield dir

  override def getResultsDirOf(project: Project): Operation[File]       = dirOf(project, "results")
  override def getCloneDirOf(project: Project): Operation[File]         = dirOf(project, "repo")
  override def getLinksDirOf(project: Project): Operation[File]         = dirOf(project, "links")
  override def getRunDirOf(project: Project): Operation[File]           = dirOf(project, "run")
  override def getOfflineBundleDirOf(project: Project): Operation[File] = dirOf(project, "offline")

  override def getProjectsOf(user: String): Operation[Set[Project]] =
    sideEffect {
      val projectsDir = new File(homeDirOf(user), "projects")
      projectsDir.mkdirs()
      projectsDir
        .listFiles()
        .filter(_.isDirectory())
        .map(dir => Project(user, dir.getName()))
        .toSet
    }

  override def createNewProject(user: String, id: Option[String]): Operation[Project] =
    operation { env =>
      for
        projects <- getProjectsOf(user)
        projectId = id.getOrElse(uid())
        exists    = projects.map(_.id).contains(projectId)
        project   = Project(user, projectId)
        status <- if (exists) getStatusOf(project) else unit(Initializing)
        _ <-
          if (exists) {
            status match
              case _: CloneFailed => delete(project)
              case _              => failIf(true)("project already exists")
          } else {
            unit(())
          }
        resultsDir <- getResultsDirOf(project)
        cloneDir   <- getCloneDirOf(project)
        linksDir   <- getLinksDirOf(project)
        runDir     <- getRunDirOf(project)
        offlineDir <- getOfflineBundleDirOf(project)
        _ <- sideEffect {
          resultsDir.mkdirs()
          cloneDir.mkdirs()
          linksDir.mkdirs()
          runDir.mkdirs()
          offlineDir.mkdirs()
        }
        _ <- writeMetadata(project, "name", "Untitled project")
        _ <- writeMetadata(project, "created", LocalDateTime.now().format(formatter))
      yield project
    }

  private def readMetadata(project: Project, key: String): Operation[String] =
    metadataRepository.read(project, key) map {
      case Some(value) => value
      case None        => ""
    }

  private def writeMetadata(project: Project, key: String, value: String): Operation[Unit] =
    metadataRepository.write(project, key, value)

  override def getNameOf(project: Project): Operation[String] =
    readMetadata(project, "name")

  override def getMemoryRequestOf(project: Project): Operation[Double] =
    operation { env =>
      readMetadata(project, "memory-request") map {
        case "" => env.defaultMemoryRequest
        case value =>
          try value.toDouble min env.maxMemoryRequest
          catch
            case t =>
              debug(s"Project ${project} has invalid memory request: $value")(using env.debugWriter)
              env.defaultMemoryRequest
      }
    }

  override def getCpuRequestOf(project: Project): Operation[Double] =
    operation { env =>
      readMetadata(project, "cpu-request") map {
        case "" => env.defaultCpuRequest
        case value =>
          try value.toDouble min env.maxCpuRequest
          catch
            case t =>
              debug(s"Project ${project} has invalid cpu request: $value")(using env.debugWriter)
              env.defaultCpuRequest
      }
    }

  override def setName(project: Project, name: String): Operation[Unit] =
    writeMetadata(project, "name", name)

  def setMemoryRequest(project: Project, value: Double): Operation[Boolean] =
    operation { env =>
      if (value <= 0 || value > env.maxMemoryRequest) unit(false)
      else writeMetadata(project, "memory-request", value.toString()).map(_ => true)
    }

  def setCpuRequest(project: Project, value: Double): Operation[Boolean] =
    operation { env =>
      if (value <= 0 || value > env.maxCpuRequest) unit(false)
      else writeMetadata(project, "cpu-request", value.toString()).map(_ => true)
    }

  override def getCreatedTimeOf(project: Project): Operation[LocalDateTime] =
    readMetadata(project, "created").map(LocalDateTime.parse(_, formatter))

  override def getSourceOf(project: Project): Operation[ProjectSource] =
    for
      sourceRaw <- readMetadata(project, "project-source")
      source <- sideEffect {
        if (sourceRaw.isBlank()) Right(SourceNotYetDefined)
        else
          io.circe.parser
            .decode[ProjectSource](sourceRaw)
            .left
            .map(s => new Error { override val message = s.toString() })
      }
    yield source

  override def setSource(project: Project, source: ProjectSource) =
    writeMetadata(project, "project-source", source.asJson.spaces2)

  override def getImageOf(project: Project): Operation[Image] =
    for
      imageRaw <- readMetadata(project, "image")
      image <- sideEffect {
        io.circe.parser
          .decode[Image](imageRaw)
          .left
          .map(s => new Error { override val message = s.toString() })
      }
    yield image

  override def setImage(project: Project, image: Image): Operation[Unit] =
    writeMetadata(project, "image", image.asJson.spaces2)

  private def patchStatus(project: Project, status: ProjectStatus): Operation[ProjectStatus] =
    operation { env =>
      if (status == Cloned)
        getSourceOf(project) map {
          case _: ShareIdSource => Ready
          case _                => status
        }
      else {
        Right(status)
      }
    }

  override def getStatusOf(project: Project): Operation[ProjectStatus] =
    operation { env =>
      for
        status <- sideEffect {
          val logDir      = getLogDir(project)
          val cloneLog    = new File(logDir, env.cloneService.cloneLogFileName)
          val cloneMarker = new File(cloneLog.getAbsolutePath() + ".rrp")
          val buildLog    = new File(logDir, "build.log")
          val buildMarker = new File(buildLog.getAbsolutePath() + ".rrp")

          if (!cloneLog.exists() && buildLog.exists()) {
            if ((!buildMarker.exists())) {
              BuildingFromArchive
            } else if (buildMarker.length() > 0) {
              BuildFromArchiveFailed
            } else {
              Ready
            }
          } else if (!cloneLog.exists()) {
            Initializing
          } else if (!cloneMarker.exists()) {
            Cloning
          } else if (cloneMarker.length() > 0) {
            CloneFailed(readFile(cloneMarker).get)
          } else if (!buildLog.exists()) {
            Cloned
          } else if (!buildMarker.exists()) {
            Building
          } else if (buildMarker.length() > 0) {
            BuildFailed
          } else {
            Ready
          }
        }
        patchedStatus <- patchStatus(project, status)
      yield patchedStatus
    }

  override def delete(project: Project): Operation[Unit] =
    for
      projectDir <- dirOf(project)
      _ <- sideEffect {
        FileUtils.deleteDirectory(projectDir)
      }
    yield ()

  override def initializeProjectFrom(share: Share, target: Project): Operation[Unit] =
    val source = new Project(share.author, share.projectId)
    for
      name  <- getNameOf(source)
      image <- getImageOf(source)
      _     <- setName(target, name)
      _     <- setImage(target, image)
      _     <- setSource(target, new ShareIdSource(share))
    yield ()
