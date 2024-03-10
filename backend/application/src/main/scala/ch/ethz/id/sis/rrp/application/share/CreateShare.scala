package ch.ethz.id.sis.rrp
package application
package project

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

case class CreateShare(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (post & path("projects" / PathMatchers.Segment / "shares")) { projectId =>
      onValidSession { session =>
        parseRequest[String] { description =>
          shareService.createShareId(Project(session.username, projectId), description) map { id =>
            respondWithHeaders(Location(s"$resourcePrefix/projects/$projectId/shares/$id")) {
              complete(StatusCodes.Created)
            }
          }
        }
      }
    }
