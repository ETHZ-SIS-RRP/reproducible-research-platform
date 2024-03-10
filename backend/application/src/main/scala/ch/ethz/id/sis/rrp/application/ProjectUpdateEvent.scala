package ch.ethz.id.sis.rrp
package application

import ch.ethz.id.sis.rrp.json.given
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.syntax._

import java.io.PrintWriter
import scala.util.Failure
import scala.util.Success

case class ProjectUpdateEvent private (
    id: String,
    status: ProjectStatus,
    source: ProjectSource,
    metadata: Map[String, String],
    cloneStatus: Option[CloneDetails],
    serverUrl: String,
    cloneLogUrl: String,
    buildLogUrl: String,
    resultsUrl: String,
    logsUrl: String
)

object ProjectUpdateEvent:
  def apply(resourcePrefix: String)(project: Project): Operation[Option[ProjectUpdateEvent]] =
    operation { env =>
      import env._
      val logic: Operation[ProjectUpdateEvent] =
        for
          status        <- projectService.getStatusOf(project)
          name          <- projectService.getNameOf(project)
          source        <- projectService.getSourceOf(project)
          created       <- projectService.getCreatedTimeOf(project)
          memoryRequest <- projectService.getMemoryRequestOf(project)
          cpuRequest    <- projectService.getCpuRequestOf(project)
          clone <-
            if (status.cloneComplete) cloneService.getDetailsOf(project).map(Some(_))
            else operationMonad.unit(None)
          processes <- processService.getProcessesOf(project)
          containerStatus = processes.find(_.processType == Server) match {
            case Some(process) => if (process.isRunning) ContainerRunning else ContainerProcessing
            case None          => ContainerNotRunning
          }
        yield ProjectUpdateEvent(
          project.id,
          status,
          source,
          Map(
            "name"           -> name,
            "created"        -> created.format(formatter),
            "memory-request" -> memoryRequest.toString(),
            "cpu-request"    -> cpuRequest.toString()
          ),
          clone,
          containerStatus match {
            case ContainerRunning    => s"${serverLocation}/${project.id}"
            case ContainerNotRunning => "not running"
            case ContainerProcessing => "processing"
          },
          s"$resourcePrefix/projects/${project.id}/logs/clone.log",
          s"$resourcePrefix/projects/${project.id}/logs/build.log",
          s"$resourcePrefix/projects/${project.id}/files",
          s"$resourcePrefix/projects/${project.id}/logs"
        )

      logic andThen {
        case Success(Right(value)) => Success(Right(Some(value)))
        case Success(Left(err)) =>
          debug(s"Creating update event for $project failed")(using env.debugWriter)
          debug(err)(using env.debugWriter)
          Success(Right(None))
        case Failure(err) =>
          debug(s"Creating update event for $project failed")(using env.debugWriter)
          debug(err)(using env.debugWriter)
          Success(Right(None))
      }
    }

given Encoder[ProjectUpdateEvent] = deriveEncoder
