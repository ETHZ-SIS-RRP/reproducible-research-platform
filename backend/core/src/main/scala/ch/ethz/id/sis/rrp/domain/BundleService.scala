package ch.ethz.id.sis.rrp
package domain

trait BundleService:
  def createBundle(project: Project, description: String): Operation[OfflineBundle]
  def listBundlesOf(project: Project): Operation[List[(OfflineBundle, BundleStatus)]]
  def deleteBundle(project: Project, fileName: String): Operation[Unit]
