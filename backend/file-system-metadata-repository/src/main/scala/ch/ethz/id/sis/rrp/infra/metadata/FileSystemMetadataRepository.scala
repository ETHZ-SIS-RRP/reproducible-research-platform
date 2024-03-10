package ch.ethz.id.sis.rrp
package infra
package metadata

import java.io.File

case class FileSystemMetadataRepository(getDir: Project => File) extends MetadataRepository:
  def write(project: Project, key: String, value: String): Operation[Unit] =
    sideEffect {
      val metadataDir = getDir(project)
      metadataDir.mkdirs()
      writeFile(new File(metadataDir, key), value)
    }

  def read(project: Project, key: String): Operation[Option[String]] =
    sideEffect {
      val metadataDir = getDir(project)
      readFile(new File(metadataDir, key))
    }
