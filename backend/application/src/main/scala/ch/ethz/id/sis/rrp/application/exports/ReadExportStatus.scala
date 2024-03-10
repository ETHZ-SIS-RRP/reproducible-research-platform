package ch.ethz.id.sis.rrp
package application
package exports

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

import java.time.LocalDateTime

case class ReadExportStatus(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (get & path("projects" / PathMatchers.Segment / "exports")) { projectId =>
      onValidSession { session =>
        val project = Project(session.username, projectId)
        for
          logs <- logService.listLogs(project).map(_.filter(_.logType == LogType.ImageExport))
          exported = logs.find(_.outcome.getOrElse(Left("fails")).isRight).isDefined
          image    = s"${env.exportRegistry}:$projectId-${session.username}"
          dataSetExports <- mountingService.readExportStatusOf(project, session)
          internalImage  <- projectService.getImageOf(project)
          exportedImage = Image(image, internalImage.buildToolName, internalImage.buildToolVersion)
          command      <- serverCommandConfiguration(exportedImage, "offline")(project)
          cloneDetails <- cloneService.getDetailsOf(project)
        yield complete(
          jsonResponse(
            ExportStatus(
              if (exported) image else "",
              command,
              cloneDetails.remote.getOrElse(""),
              cloneDetails.commit.hash,
              dataSetExports.transform((_, value) => (value._1, value._2.getOrElse("")))
            )
          )
        )
      }
    }
