package ch.ethz.id.sis.rrp
package application
package logs

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneId

import ch.ethz.id.sis.rrp.json.given

case class ListLogs(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:
  import env._

  override def route(resourcePrefix: String) =
    (get & path("projects" / PathMatchers.Segment / "logs")) { projectId =>
      onValidSession { session =>
        for
          logs <- logService.listLogs(Project(session.username, projectId))
          entries = logs.map(log =>
            new LogEntryReference(
              log.id,
              s"$resourcePrefix/projects/$projectId/logs/${log.id}",
              log.logType.toString(),
              log.timestamp.format(formatter),
              log.outcome.map(_.isRight),
              log.outcome.map(_.swap.toOption).flatten.getOrElse("")
            )
          )
        yield complete(jsonResponse(entries))
      }
    }
