package ch.ethz.id.sis.rrp
package application
package container

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

case class CommandContainer(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:
  import env._

  override def route(resourcePrefix: String) =
    (post & path("projects" / PathMatchers.Segment / "command")) { projectId =>
      onValidSession { session =>
        parseRequest[ProjectCommand] { command =>
          val project = new Project(session.username, projectId)
          command.op match
            case "start" =>
              for
                spec <- env.processService.startServer(session, project)
                _    <- env.processService.execute(spec)
              yield complete(StatusCodes.OK)
            case "stop" => {
              for {
                projects <- env.processService.getProcessesOf(project)
                _ <- projects.find(_.processType == Server).map(_.processId) match {
                  case Some(processId) => env.processService.stop(processId)
                  case None            => unit(())
                }
              } yield complete(StatusCodes.OK)
            }
            case unknown => sideEffect { complete(jsonResponse(s"Unknown command $unknown", StatusCodes.BadRequest)) }
        }
      }
    }
