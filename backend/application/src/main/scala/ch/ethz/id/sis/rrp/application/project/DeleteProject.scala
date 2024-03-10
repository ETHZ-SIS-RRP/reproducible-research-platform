package ch.ethz.id.sis.rrp
package application
package project

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

case class DeleteProject(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:
  import env._

  override def route(resourcePrefix: String) =
    (delete & path("projects" / PathMatchers.Segment)) { projectId =>
      onValidSession { session =>
        projectService.delete(Project(session.username, projectId)).map(_ => complete(StatusCodes.OK))
      }
    }
