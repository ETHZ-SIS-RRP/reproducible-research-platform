package ch.ethz.id.sis.rrp
package domain

trait ShareService:

  object ShareNotFound extends Error {
    override val message = "Share not found"
  }

  def createShareId(project: Project, description: String): Operation[String]
  def listSharesOf(project: Project): Operation[List[Share]]
  def getShare(shareId: String): Operation[Share]
  def deleteShare(project: Project, shareId: String): Operation[Unit]
