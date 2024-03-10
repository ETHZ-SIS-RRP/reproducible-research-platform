package ch.ethz.id.sis.rrp
package application
package authentication

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter

import java.time.LocalDateTime
import akka.http.javadsl.model.StatusCode

case class Authenticate(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  override def route(resourcePrefix: String) =
    (post & path("authentication")) {
      onAnySession { _ =>
        parseRequest[LoginRequest] {
          case request: CredentialsLoginRequest =>
            authenticationService.authenticate(Credentials(request.username, request.password)) map {
              case Some((openbisToken, username, validTo)) =>
                val sessionInfo  = SessionInfo(username, openbisToken, validTo)
                val sessionToken = cookieJar.storeSession(sessionInfo)
                setCookie(HttpCookie(sessionCookieName, value = sessionToken, path = Some("/"))) {
                  complete(jsonResponse(Map[String, String]()))
                }
              case None => complete(StatusCodes.Unauthorized)
            }
          case request: PersonalAccessTokenLoginRequest =>
            authenticationService.authenticate(request.token) map {
              case Some((openbisToken, username, validTo)) =>
                val sessionInfo  = SessionInfo(username, openbisToken, validTo)
                val sessionToken = cookieJar.storeSession(sessionInfo)
                setCookie(HttpCookie(sessionCookieName, value = sessionToken, path = Some("/"))) {
                  complete(jsonResponse(Map[String, String]()))
                }
              case None => complete(StatusCodes.Unauthorized)
            }
        }
      }
    }
