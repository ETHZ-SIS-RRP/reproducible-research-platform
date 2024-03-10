package ch.ethz.id.sis.rrp
package application
package project

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneId

case class ReadFiles(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:
  import env._

  private def getContent(f: File): FileSystemNode = {
    val path         = f.toPath()
    val name         = f.getName()
    val size         = Files.size(path)
    val modification = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault())

    if (f.isDirectory()) {
      val content = f.listFiles().toList.filter(!_.getName().startsWith("."))
      DirectoryNode(name, modification, content.map(getContent))
    } else {
      FileNode(name, modification, size)
    }
  }

  override def route(resourcePrefix: String) =
    (get & path("projects" / PathMatchers.Segment / "files")) { projectId =>
      onValidSession { session =>
        for
          resultsDir <- projectService.getResultsDirOf(new Project(session.username, projectId))
          content    <- sideEffect { getContent(resultsDir) }
        yield complete(jsonResponse(content))
      }
    }
