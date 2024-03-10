package ch.ethz.id.sis.rrp
package application
package exports

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

import java.time.LocalDateTime

case class ExportImage(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (post & path("projects" / PathMatchers.Segment / "exports")) { projectId =>
      onValidSession { session =>
        val project = Project(session.username, projectId)
        val start = for
          spec    <- processService.exportImage(session, project)
          logFile <- processService.execute(spec)
        yield logFile
        start.map(logFile =>
          respondWithHeaders(Location(s"$resourcePrefix/$projectId/logs/$logFile")) {
            complete(StatusCodes.Created)
          }
        )
      }
    }
