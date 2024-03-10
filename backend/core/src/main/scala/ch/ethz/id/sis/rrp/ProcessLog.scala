package ch.ethz.id.sis.rrp

import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

case class ProcessLog(file: File) {
  val finishFile = new File(file.getAbsolutePath() + ".rrp")
  def closeWithSuccess(): Unit = {
    finishFile.createNewFile()
  }

  def closeWithError(error: String) = {
    Files.write(Paths.get(finishFile.getAbsolutePath()), error.getBytes(StandardCharsets.UTF_8))
  }
}

case class ProcessLogOutputStream(file: File) extends FileOutputStream(file):
  override def close() =
    super.close()
    new File(file.getAbsolutePath() + ".rrp").createNewFile()
