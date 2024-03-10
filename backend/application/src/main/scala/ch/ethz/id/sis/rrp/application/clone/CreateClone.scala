package ch.ethz.id.sis.rrp
package application
package clone

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter
import java.io.File
import java.nio.file.Files

case class CreateClone(env: ExecutionEnvironment)(using
    AdapterConfig
) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (post & path("clones")) {
      onValidSession { session =>
        parseRequest[GitCloneRequest] { request =>
          val credentials =
            for
              username <- request.username
              password <- request.password
            yield Credentials(username, password)

          for
            project <- projectService.createNewProject(session.username, Some(request.projectId))
            remote  <- cloneService.createRemote(request.repository)
            _       <- cloneService.createClone(project, remote, credentials)
            _ <- request.shareId match
              case None =>
                for
                  _ <- projectService.setName(project, request.repository.split("/").last)
                  _ <- projectService.setSource(project, new GitSource(request.repository, ""))
                yield ()
              case Some(shareId) =>
                for
                  share <- shareService.getShare(shareId)
                  _     <- projectService.initializeProjectFrom(share, project)
                yield ()
          yield respondWithHeaders(Location(s"$resourcePrefix/projects/${project.id}/logs/clone.log")) {
            complete(StatusCodes.Created)
          }
        }
      }
    }
