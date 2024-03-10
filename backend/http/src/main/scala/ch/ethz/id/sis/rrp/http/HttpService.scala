package ch.ethz.id.sis.rrp
package http

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Host
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.DebuggingDirectives
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

case class HttpService(port: Int, actorSystem: ActorSystem, adapters: HttpAdapter*):

  given ActorSystem              = actorSystem
  given ExecutionContextExecutor = actorSystem.dispatcher

  val config      = ConfigFactory.load()
  lazy val logger = Logging(actorSystem, getClass().getName())

  val extractHostPort: HttpHeader => Option[Host] =
    case h: Host => Some(h)
    case x @ _   => None

  val route: String => Route = resourcePrefix =>
    adapters.tail.foldLeft(adapters.head.route(resourcePrefix)) { _ ~ _.route(resourcePrefix) }

  val fullRoute = headerValue(extractHostPort) { host =>
    optionalHeaderValueByName("X-Forwarded-Proto") { proto =>
      val resourcePrefix = proto.getOrElse("http") + "://" + host.host +
        (if (host.port == 80 || host.port == 0) "" else ":" + host.port)
      ignoreTrailingSlash {
        route(resourcePrefix)
      }
    }
  }

  lazy val loggedRoute = DebuggingDirectives.logRequestResult(("", Logging.InfoLevel))(fullRoute)

  def run(): Future[Http.ServerBinding] =
    val bind = Http().bindAndHandle(
      loggedRoute,
      config.getString("http.interface"),
      port
    )
    log("HTTP server listening on port " + port)
    bind
