package ch.ethz.id.sis.rrp
package application
package bundle

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

case class DeleteBundle(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (delete & path("projects" / PathMatchers.Segment / "bundles" / PathMatchers.Segment)) { (projectId, fileName) =>
      onValidSession { session =>
        bundleService.deleteBundle(Project(session.username, projectId), fileName).map(_ => complete(StatusCodes.OK))
      }
    }
