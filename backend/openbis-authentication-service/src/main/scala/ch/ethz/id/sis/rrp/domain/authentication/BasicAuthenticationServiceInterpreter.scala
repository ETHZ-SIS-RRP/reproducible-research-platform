package ch.ethz.id.sis.rrp
package domain
package authentication

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi

import java.time.LocalDateTime

case class BasicAuthenticationServiceInterpreter(openbis: IApplicationServerApi) extends AuthenticationService:

  def authenticate(credentials: Credentials): Operation[AuthenticationResponse] =
    sideEffect {
      val token = openbis.login(credentials.username, credentials.password)
      if (token == null)
        None
      else
        Some((token, credentials.username, LocalDateTime.now()))
    }

  def authenticate(pat: String): Operation[AuthenticationResponse] =
    unit(None)
