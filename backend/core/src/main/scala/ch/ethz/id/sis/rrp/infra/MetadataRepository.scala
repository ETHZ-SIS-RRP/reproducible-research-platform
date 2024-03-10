package ch.ethz.id.sis.rrp
package infra

trait MetadataRepository:
  def write(project: Project, key: String, data: String): Operation[Unit]
  def read(project: Project, key: String): Operation[Option[String]]
