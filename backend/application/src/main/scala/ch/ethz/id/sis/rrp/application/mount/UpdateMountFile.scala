package ch.ethz.id.sis.rrp
package application
package mount

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

case class UpdateMountFile(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (post & path("projects" / PathMatchers.Segment / "mounts")) { projectId =>
      onValidSession { session =>
        parseRequest[String] { data =>
          mountingService.updateMountConfigOf(new Project(session.username, projectId), data, session) map (_ =>
            complete(StatusCodes.OK)
          )
        }
      }
    }
