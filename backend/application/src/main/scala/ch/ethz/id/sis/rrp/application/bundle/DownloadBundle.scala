package ch.ethz.id.sis.rrp
package application
package bundle

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter
import ch.ethz.id.sis.rrp.json.given
import java.time.LocalDateTime
import java.io.File

case class DownloadBundle(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (get & path("projects" / PathMatchers.Segment / "bundles" / PathMatchers.Segment)) { (projectId, fileName) =>
      onValidSession { session =>
        for {
          bundleDir <- projectService.getOfflineBundleDirOf(Project(session.username, projectId))
          bundleFile = new File(bundleDir, fileName)
        } yield getFromFile(bundleFile)
      }
    }
