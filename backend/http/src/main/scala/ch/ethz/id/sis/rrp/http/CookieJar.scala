package ch.ethz.id.sis.rrp
package http

trait CookieJar:
  def storeSession(info: SessionInfo): String
  def deleteSession(id: String): Unit
  def getSession(id: String): Option[SessionInfo]
