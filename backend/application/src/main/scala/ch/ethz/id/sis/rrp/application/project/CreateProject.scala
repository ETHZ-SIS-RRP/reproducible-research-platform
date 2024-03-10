package ch.ethz.id.sis.rrp
package application
package project

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import akka.http.scaladsl.server.Route

case class CreateProject(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (post & path("projects")) {
      onValidSession { session =>
        parseRequest[CreateProjectRequest] { request =>
          val project = Project(session.username, request.projectId)
          request match
            case CreateProjectFromArchiveRequest(projectId, archiveId) => createFromArchive(project, archiveId, session)
            case CreateProjectFromCloneRequest(name, projectId, branch) => createFromClone(project, name, branch)
        }
      }
    }

  private def createFromArchive(project: Project, archiveId: String, session: SessionInfo): Operation[Route] =
    for {
      _ <- projectService.createNewProject(project.user, Some(project.id))
      spec <- processService.buildProjectFromArchive(session, project, archiveId) failWith projectService.delete(
        project
      )
      _                 <- logService.createLog(project, "build.log", s"Starting build from archive $archiveId...\n")
      (name, imageName) <- processService.execute(spec)
      _                 <- projectService.setName(project, name)
      _                 <- projectService.setImage(project, imageName)
      _                 <- projectService.setSource(project, ArchiveSource(archiveId))
    } yield complete(StatusCodes.OK)

  private def createFromClone(project: Project, name: String, branch: String): Operation[Route] =
    for
      _            <- cloneService.checkout(project, branch)
      cloneDetails <- cloneService.getDetailsOf(project)
      repo = cloneDetails.remote.getOrElse("unknown")
      _     <- logService.createLog(project, "build.log", s"Starting build from ${repo}...\n")
      spec  <- processService.buildProjectFromGit(project)
      image <- processService.execute(spec)
      _     <- projectService.setName(project, name)
      _     <- projectService.setSource(project, GitSource(cloneDetails.remote.get, cloneDetails.commit.hash))
      _     <- projectService.setImage(project, image)
    yield complete(StatusCodes.OK)
