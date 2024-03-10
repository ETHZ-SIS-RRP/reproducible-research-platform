package ch.ethz.id.sis.rrp
package application
package bundle

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter
import java.time.LocalDateTime

case class ListBundles(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (get & path("projects" / PathMatchers.Segment / "bundles")) { projectId =>
      onValidSession { session =>
        bundleService.listBundlesOf(Project(session.username, projectId)) map { bundles =>
          complete(jsonResponse(bundles map { (bundle, status) =>
            BundleInfo(
              bundle.description,
              bundle.createdAt,
              status.successful,
              status.finished,
              status.size,
              bundle.commit.hash,
              bundle.commit.message,
              s"$resourcePrefix/projects/${projectId}/bundles/${bundle.name}.zip"
            )
          }))
        }
      }
    }
