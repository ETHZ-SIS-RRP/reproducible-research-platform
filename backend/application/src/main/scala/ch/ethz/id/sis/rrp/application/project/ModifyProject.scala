package ch.ethz.id.sis.rrp
package application
package project

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import akka.http.scaladsl.server.Route
import akka.http.javadsl.model.StatusCode

case class ModifyProject(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (post & path("projects" / Segment)) { projectId =>
      onValidSession { session =>
        parseRequest[ModifyProjectRequest] { request =>
          val project = Project(session.username, projectId)
          val numeric = request.value.toDoubleOption
          for {
            error <- (request.key, numeric) match
              case ("name", _) => projectService.setName(project, request.value).map(_ => None)
              case ("memory-request", Some(value)) =>
                projectService
                  .setMemoryRequest(project, value)
                  .map(if (_) None else Some(s"Invalid value for memory request: ${request.value}"))
              case ("cpu-request", Some(value)) =>
                projectService
                  .setCpuRequest(project, value)
                  .map(if (_) None else Some(s"Invalid value for CPU request: ${request.value}"))
              case ("cpu-request", None) | ("memory-request", None) => unit(Some("Non-numeric value"))
              case _                                                => unit(Some(s"unknown key ${request.key}"))
          } yield error match
            case None        => complete(StatusCodes.OK)
            case Some(error) => complete(jsonResponse(ErrorResponse(error), StatusCodes.BadRequest))
        }
      }
    }
