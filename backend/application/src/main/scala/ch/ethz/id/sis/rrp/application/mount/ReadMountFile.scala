package ch.ethz.id.sis.rrp
package application
package mount

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

case class ReadMountFile(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (get & path("projects" / PathMatchers.Segment / "mounts")) { projectId =>
      onValidSession { session =>
        mountingService.readMountConfigOf(new Project(session.username, projectId)) map { data =>
          complete(jsonResponse(data))
        }
      }
    }
