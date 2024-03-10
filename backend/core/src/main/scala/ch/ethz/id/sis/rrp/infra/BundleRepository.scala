package ch.ethz.id.sis.rrp
package infra

import java.time.LocalDateTime
import java.io.File

trait BundleRepository:
  def createBundle(project: Project, description: String, commit: CommitDetails): Operation[OfflineBundle]
  def listBundles(project: Project): Operation[List[(OfflineBundle, BundleStatus)]]
  def deleteBundle(project: Project, fileName: String): Operation[Unit]
