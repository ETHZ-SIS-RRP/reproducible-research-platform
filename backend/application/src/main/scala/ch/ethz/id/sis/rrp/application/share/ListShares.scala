package ch.ethz.id.sis.rrp
package application
package project

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter
import ch.ethz.id.sis.rrp.json.given

case class ListShares(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (get & path("projects" / PathMatchers.Segment / "shares")) { projectId =>
      onValidSession { session =>
        shareService.listSharesOf(Project(session.username, projectId)) map { shares =>
          complete(jsonResponse(shares))
        }
      }
    }
