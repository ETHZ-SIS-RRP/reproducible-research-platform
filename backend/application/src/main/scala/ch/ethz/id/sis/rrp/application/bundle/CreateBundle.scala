package ch.ethz.id.sis.rrp
package application
package bundle

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

case class CreateBundle(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (post & path("projects" / PathMatchers.Segment / "bundles")) { projectId =>
      onValidSession { session =>
        parseRequest[String] { description =>
          val startBuild =
            for {
              spec    <- processService.createOfflineBundle(session, Project(session.username, projectId), description)
              logFile <- processService.execute(spec)
            } yield logFile
          startBuild.map(logFile =>
            respondWithHeaders(Location(s"$resourcePrefix/projects/$projectId/logs/$logFile")) {
              complete(StatusCodes.Created)
            }
          )
        }
      }
    }
