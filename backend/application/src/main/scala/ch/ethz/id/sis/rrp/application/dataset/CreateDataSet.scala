package ch.ethz.id.sis.rrp
package application
package dataset

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import akka.http.scaladsl.server.Route
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter
import ch.ethz.id.sis.rrp.json.given

case class CreateDataSet(env: ExecutionEnvironment)(using
    AdapterConfig
) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (post & path("projects" / PathMatchers.Segment / "dataset")) { projectId =>
      onValidSession { session =>
        parseRequest[CreateDataSetRequest] { request =>
          if (request.projectArchive) {
            archiveService
              .archiveProject(
                new Project(session.username, projectId),
                request.dataSetType,
                request.entityId,
                request.properties,
                session
              )
              .map(id =>
                respondWithHeaders(Location(s"$resourcePrefix/projects/$projectId/logs/$id")) {
                  complete(StatusCodes.Created)
                }
              )
          } else {
            archiveService
              .archiveResults(
                new Project(session.username, projectId),
                request.dataSetType,
                request.entityId,
                request.properties,
                session
              )
              .map(id =>
                respondWithHeaders(Location(s"$resourcePrefix/projects/$projectId/logs/$id")) {
                  complete(StatusCodes.Created)
                }
              )
          }
        }
      }
    }
