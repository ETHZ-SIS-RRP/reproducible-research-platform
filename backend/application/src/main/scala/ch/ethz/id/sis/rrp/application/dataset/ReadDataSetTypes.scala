package ch.ethz.id.sis.rrp
package application
package dataset

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter
import ch.ethz.id.sis.rrp.json.given

case class ReadDataSetTypes(env: ExecutionEnvironment)(using
    AdapterConfig
) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (get & path("dataset-types")) {
      onValidSession { session =>
        archiveService
          .listArchiveTypes(session)
          .map(types => complete(jsonResponse(types)))
      }
    }
