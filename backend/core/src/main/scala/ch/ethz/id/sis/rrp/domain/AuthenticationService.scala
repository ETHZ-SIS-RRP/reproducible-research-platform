package ch.ethz.id.sis.rrp
package domain

import java.io.File
import java.time.LocalDateTime

trait AuthenticationService:
  type SessionToken           = String
  type UserName               = String
  type ValidUntil             = LocalDateTime
  type AuthenticationResponse = Option[(SessionToken, UserName, ValidUntil)]

  def authenticate(credentials: Credentials): Operation[AuthenticationResponse]
  def authenticate(pat: String): Operation[AuthenticationResponse]
