package ch.ethz.id.sis.rrp
package domain
package log

import org.scalatest.funsuite.AnyFunSuite

import java.io.File
import scala.io.Source
import java.util.concurrent.Semaphore

class LogReadingTest extends AnyFunSuite:

  test("Both line feeds and carriage returns terminate a line") {
    val file   = new File("src/test/resources/test.log")
    val result = FileSystemLogServiceInterpreter.readLogFile(file)
    assert(result.lines.length == 8)
  }

  case class Collector():
    var lines     = List[String]()
    val semaphore = new Semaphore(0)
    def collect(line: NewLogLine): Unit =
      line match {
        case NewLogLine(_, Some("")) => semaphore.release()
        case NewLogLine(_, Some(error)) =>
          lines = lines.appended(error)
          semaphore.release()
        case NewLogLine(line, None) => lines = lines.appended(line)
      }

    def waitFor(): Unit = semaphore.acquire()

  test("Tailing successful run") {
    val file      = new File("src/test/resources/success.log")
    val collector = Collector()

    FileSystemLogServiceInterpreter.tailLogFile(file, 4, collector.collect)
    collector.waitFor()
    assert(collector.lines.length == 5)
  }

  test("Tailing failing run") {
    val file      = new File("src/test/resources/fail.log")
    val collector = Collector()

    FileSystemLogServiceInterpreter.tailLogFile(file, 4, collector.collect)
    collector.waitFor()
    assert(collector.lines.length == 6) // extra line is the error message
  }
