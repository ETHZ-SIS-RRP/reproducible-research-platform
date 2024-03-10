package ch.ethz.id.sis.rrp
package application
package logs

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

import java.io.File
import akka.http.scaladsl.model.StatusCodes

case class ReadLog(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:
  import env._

  override def route(resourcePrefix: String) =
    (get & path("projects" / PathMatchers.Segment / "logs" / PathMatchers.Segment)) { (projectId, fileId) =>
      onValidSession { session =>
        val logic =
          for
            log <- logService.readLog(Project(session.username, projectId), fileId)
            tailUrl = s"$resourcePrefix/projects/$projectId/logs/$fileId/tail/${log.lines.length + 1}"
          yield complete(jsonResponse(JsonLogResponse(log.lines, log.error, if (log.hasMore) Some(tailUrl) else None)))

        logic onError { _ => complete(StatusCodes.NotFound) }
      }
    }
