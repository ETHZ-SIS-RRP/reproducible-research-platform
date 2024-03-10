package ch.ethz.id.sis.rrp
package application
package authentication

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

case class CheckSession(
    env: ExecutionEnvironment
)(using
    AdapterConfig
) extends HttpAdapter:

  import env._
  given Traversable[Set] = setTraversable

  override def route(resourcePrefix: String) =
    (get & path("authentication")) {
      onValidSession { session =>
        for
          projects   <- projectService.getProjectsOf(session.username)
          detailsRaw <- projects.map(ProjectUpdateEvent(resourcePrefix)).sequence
          details = detailsRaw collect { case Some(value) => value }
        yield complete(
          jsonResponse(SessionCheckResponse(session.username, formatter.format(session.validTo), details.toList))
        )
      }
    }
