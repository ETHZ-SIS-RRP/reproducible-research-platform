package ch.ethz.id.sis.rrp
package application
package authentication

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

case class EndSession(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  override def route(resourcePrefix: String) =
    (delete & path("authentication")) {
      onAnySession { cookie =>
        sideEffect {
          cookie match
            case None => complete(StatusCodes.OK)
            case Some(c) =>
              cookieJar.deleteSession(c.value)
              setCookie(HttpCookie(sessionCookieName, value = "", path = Some("/"))) {
                complete(StatusCodes.OK)
              }
        }
      }
    }
