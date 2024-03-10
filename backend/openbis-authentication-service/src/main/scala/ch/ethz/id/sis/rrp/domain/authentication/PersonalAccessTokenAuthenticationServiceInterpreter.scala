package ch.ethz.id.sis.rrp
package domain
package authentication

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.PersonalAccessToken
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.create.PersonalAccessTokenCreation
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.fetchoptions.PersonalAccessTokenFetchOptions
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.id.PersonalAccessTokenPermId
import ch.ethz.sis.openbis.generic.asapi.v3.dto.pat.search.PersonalAccessTokenSearchCriteria
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.IPersonId
import ch.ethz.sis.openbis.generic.asapi.v3.dto.person.id.PersonPermId
import ch.ethz.sis.openbis.generic.asapi.v3.dto.session.SessionInformation
import ch.systemsx.cisd.common.exceptions.InvalidSessionException

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.ArrayList
import java.util.Date
import scala.jdk.CollectionConverters._

case class PersonalAccessTokenAuthenticationServiceInterpreter(openbis: IApplicationServerApi)
    extends AuthenticationService:

  private def getPatDetails(pat: String): PersonalAccessToken =
    val searchCriteria = new PersonalAccessTokenSearchCriteria()
    searchCriteria.withId().thatEquals(new PersonalAccessTokenPermId(pat))

    openbis
      .searchPersonalAccessTokens(
        pat,
        searchCriteria,
        new PersonalAccessTokenFetchOptions()
      )
      .getObjects
      .get(0)

  def authenticate(credentials: Credentials): Operation[AuthenticationResponse] =
    sideEffect {
      val token = openbis.login(credentials.username, credentials.password)

      if (token == null) None
      else
        val searchCriteria = new PersonalAccessTokenSearchCriteria()
        searchCriteria.withOwner().withUserId().thatEquals(credentials.username)
        searchCriteria.withSessionName().thatEquals("rrp");
        val fetchOptions = new PersonalAccessTokenFetchOptions()
        fetchOptions.withOwner()

        val res = openbis
          .searchPersonalAccessTokens(token, searchCriteria, fetchOptions)
          .getObjects()
          .asScala
          .toList
          .sortBy(_.getValidToDate())
          .reverse
          .filter(token => (token.getValidToDate(): LocalDateTime).isAfter(LocalDateTime.now().plusDays(1)))

        val pat =
          if (res.isEmpty)
            val creation = new PersonalAccessTokenCreation()
            creation.setSessionName("rrp")
            creation.setValidFromDate(
              Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).minusHours(1).toInstant())
            )
            creation.setValidToDate(new Date(Long.MaxValue))
            openbis.createPersonalAccessTokens(token, java.util.Collections.singletonList(creation)).get(0).getPermId()
          else res(0).getPermId().getPermId()

        val sessionInfo            = openbis.getSessionInformation(pat)
        val details                = getPatDetails(pat)
        val validTo: LocalDateTime = details.getValidToDate()
        Some(pat, sessionInfo.getUserName(), validTo)
    }

  def authenticate(pat: String): Operation[AuthenticationResponse] =
    sideEffect {
      try
        val sessionInfo = openbis.getSessionInformation(pat)
        val details     = getPatDetails(pat)

        val validFrom: LocalDateTime = details.getValidFromDate()
        val validTo: LocalDateTime   = details.getValidToDate()

        Some(pat, sessionInfo.getUserName, validTo)
      catch case e: InvalidSessionException => None
    }
