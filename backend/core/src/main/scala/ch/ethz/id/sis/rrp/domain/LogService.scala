package ch.ethz.id.sis.rrp

import java.io.Closeable

trait LogService:
  def listLogs(project: Project): Operation[List[LogEntry]]
  def readLog(project: Project, id: String): Operation[LogFileContent]
  def tailLog(project: Project, id: String, startFrom: Int, f: NewLogLine => Unit): Operation[Closeable]
  def createLog(project: Project, id: String, content: String): Operation[Unit]
