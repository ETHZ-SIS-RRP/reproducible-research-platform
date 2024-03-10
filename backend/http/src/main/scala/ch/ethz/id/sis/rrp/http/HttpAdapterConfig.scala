package ch.ethz.id.sis.rrp
package http

import akka.actor.ActorSystem

case class AdapterConfig(sessionCookieName: String, cookieJar: CookieJar, actorSystem: ActorSystem)
