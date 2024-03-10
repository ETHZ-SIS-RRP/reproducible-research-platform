package ch.ethz.id.sis.rrp
package domain
package log

import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListenerAdapter

import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Scanner
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import scala.io.Source

case class FileSystemLogServiceInterpreter(getDir: Project => File) extends LogService:
  import FileSystemLogServiceInterpreter._

  private def getLogFile(project: Project, id: String): Operation[File] = sideEffect {
    val file = new File(getDir(project), id)
    if (file.exists() && !id.contains("..") && !id.contains("/")) Right(file)
    else Left(Error("Not found"))
  }

  override def listLogs(project: Project): Operation[List[LogEntry]] =
    operation { env =>
      for
        source <- env.projectService.getSourceOf(project)
        isArchive = source match
          case ArchiveSource(_) => true
          case _                => false
        result <- sideEffect {
          val logDir = getDir(project)
          logDir
            .listFiles()
            .toList
            .filter(file => file.getName().endsWith(".log") && !file.isDirectory)
            .map(file =>
              val id = file.getName()
              val logType = id match
                case "build.log" => if (isArchive) LogType.UnarchiveProject else LogType.Build
                case "clone.log" => LogType.Clone
                case _ =>
                  val ts   = id.substring(0, 17)
                  val name = id.substring(18)
                  name match
                    case "archive-project.log"                 => LogType.ArchiveProject
                    case "archive-results.log"                 => LogType.ArchiveResults
                    case "server.log"                          => LogType.Server
                    case "export.log"                          => LogType.ImageExport
                    case u if u.endsWith("-player-bundle.log") => LogType.CreateOfflineBundle

              val attr      = Files.readAttributes(file.toPath(), classOf[BasicFileAttributes]);
              val timestamp = LocalDateTime.ofInstant(attr.creationTime().toInstant(), ZoneId.systemDefault())

              val markerFile = new File(file.getAbsolutePath() + ".rrp")
              val status =
                if (markerFile.exists() && markerFile.lastModified() < new Date().getTime() + 1000)
                  val error = scala.io.Source.fromFile(markerFile).getLines().mkString
                  if (error.isBlank())
                    Some(Right(()))
                  else
                    Some(Left(error))
                else None
              LogEntry(id, logType, timestamp, status)
            )
        }
      yield result
    }

  override def createLog(project: Project, id: String, content: String): Operation[Unit] =
    sideEffect {
      val logDir = getDir(project)
      logDir.mkdirs()
      writeFile(new File(logDir, id), content)
    }

  override def readLog(project: Project, id: String): Operation[LogFileContent] =
    for {
      file    <- getLogFile(project, id)
      content <- sideEffect { readLogFile(file) }
    } yield content

  override def tailLog(project: Project, id: String, startLine: Int, f: NewLogLine => Unit): Operation[Closeable] =
    for {
      file    <- getLogFile(project, id)
      content <- sideEffect { tailLogFile(file, startLine, f) }
    } yield content

object FileSystemLogServiceInterpreter:
  def readLogFile(logFile: File): LogFileContent =
    val finishFile = new File(logFile.getAbsolutePath() + ".rrp")
    val hasMore    = !finishFile.exists()

    var lines   = List[String]()
    val scanner = new Scanner(logFile, "UTF-8");
    scanner.useDelimiter("\n")
    while (scanner.hasNext()) {
      val next = scanner.next()
      next.split("\r") foreach { line =>
        lines = line :: lines
      }
    }

    val error =
      if (hasMore) None
      else {
        val msg = Source.fromFile(finishFile).getLines().mkString.trim()
        if (msg.isBlank()) None else Some(msg)
      }

    LogFileContent(lines.reverse, error, hasMore)

  def tailLogFile(logFile: File, startLine: Int, f: NewLogLine => Unit): Closeable =
    var lineNumber = 0
    val listener = new TailerListenerAdapter():
      override def handle(line: String): Unit =
        lineNumber += 1
        if (lineNumber >= startLine)
          f(NewLogLine(line, None))

    val ex = new ScheduledThreadPoolExecutor(1)

    val task: Runnable = new Runnable {
      val markerFile = new File(logFile.getAbsolutePath() + ".rrp")

      def run() =
        if (markerFile.exists() && markerFile.lastModified() < new Date().getTime() + 1000)
          val error = scala.io.Source.fromFile(markerFile).getLines().mkString
          f(NewLogLine("", Some(error)))
          ex.shutdown()
    }

    ex.scheduleAtFixedRate(task, 2, 2, TimeUnit.SECONDS)

    val tailer = Tailer.create(logFile, listener, 1000)
    new Closeable {
      def close(): Unit =
        ex.shutdown()
        tailer.stop()
    }
