package ch.ethz.id.sis.rrp
package domain
package clone

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.util.ByteString
import ch.ethz.id.sis.rrp.json.given
import io.circe.Encoder
import io.circe.Json

import java.net.URL
import scala.concurrent.Await
import scala.concurrent.duration._

private case class GitLabExtension(actorSystem: ActorSystem):

  given ActorSystem = actorSystem
  val http          = Http()

  private def httpRequest(url: String, data: String = null): Option[Json] = {
    val request = if (data == null) {
      HttpRequest(uri = url)
    } else {
      HttpRequest(uri = url + "?" + data, method = HttpMethods.POST)
    }
    val response = Await.result(http.singleRequest(request), 1 seconds)
    val body     = Await.result(response.entity.dataBytes.runFold(ByteString(""))(_ ++ _), 1 seconds)
    io.circe.parser.parse(body.utf8String).toOption
  }

  def getUserInfo(
      repo: String,
      credentials: Option[Credentials]
  ): Option[(Option[String], Option[String])] = {
    credentials match {
      case Some(Credentials(repoUser, password)) =>
        try {
          val repoUrl  = new URL(repo)
          val protocol = repoUrl.getProtocol()
          val host     = repoUrl.getHost()

          for {
            root <- httpRequest(
              s"$protocol://$host/oauth/token",
              s"grant_type=password&username=$repoUser&password=$password"
            ).asObject
            token <- root("access_token").asString
            user  <- httpRequest(s"$protocol://$host/api/v4/user?access_token=$token").asObject
          } yield (user("name").asString, user("email").asString)

        } catch {
          case t: Throwable => t.printStackTrace; None
        }
      case _ => None
    }
  }
