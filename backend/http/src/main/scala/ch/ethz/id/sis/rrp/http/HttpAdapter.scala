package ch.ethz.id.sis.rrp
package http

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.model.headers.HttpCookiePair
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directive
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatcher
import akka.http.scaladsl.server.PathMatchers
import akka.http.scaladsl.server.RequestContext
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.alpakka.file.scaladsl.FileTailSource
import akka.stream.scaladsl.Source
import io.circe.CursorOp
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.ParsingFailure
import io.circe.generic.semiauto._
import org.apache.commons.io.FileUtils
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListenerAdapter
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import java.io.File
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

trait HttpAdapter(using config: AdapterConfig):
  import HttpAdapter._

  val env: ExecutionEnvironment
  def route(resourcePrefix: String): Route
  var errorManager: Error => Route = err => complete(jsonResponse(ErrorResponse(err.message), StatusCodes.BadRequest))

  given actorSystem: ActorSystem = config.actorSystem
  given ExecutionContextExecutor = actorSystem.dispatcher
  val sessionCookieName          = config.sessionCookieName
  val cookieJar                  = config.cookieJar

  case class ErrorResponse(error: String)
  given Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]

  def jsonResponse[A](data: A, status: StatusCode = StatusCodes.OK)(using enc: Encoder[A]): HttpResponse =
    HttpResponse(
      entity = HttpEntity(
        ContentType(MediaTypes.`application/json`),
        enc(data).deepDropNullValues.spaces2
      ),
      status = status
    )

  val errorToString: io.circe.Error => String =
    case DecodingFailure(msg, ops) =>
      val path = CursorOp.opsToPath(ops)
      s"JSON content error at $$$path: $msg"
    case ParsingFailure(msg, _) => s"JSON syntax error: $msg"
    case other                  => s"Internal error: ${other.getMessage()}"

  def parseRequest[A](using decoder: Decoder[A])(f: A => Operation[Route]): Operation[Route] =
    sideEffect { (context: RequestContext) =>
      Unmarshal(context.request.entity)
        .to[String]
        .flatMap(content =>
          io.circe.parser.decode[A](content).left.map(errorToString) match
            case Left(err)    => complete(jsonResponse(ErrorResponse(err), StatusCodes.BadRequest))(context)
            case Right(input) => exec(f(input))(context)
        )
    }

  def onValidSession(f: SessionInfo => Operation[Route]): Route =
    cookie(config.sessionCookieName) { (cookie: HttpCookiePair) =>
      config.cookieJar.getSession(cookie.value) match {
        case Some(session) => exec(f(session))
        case None          => complete(StatusCodes.Unauthorized)
      }
    }

  def onAnySession(f: Option[HttpCookiePair] => Operation[Route]): Route =
    optionalCookie(config.sessionCookieName) { cookieOpt =>
      exec(f(cookieOpt))
    }

  private def exec(route: Operation[Route]): Route =
    route(env) match
      case Success(Right(route)) => route
      case Success(Left(err))    => errorManager(err)
      case Failure(err) =>
        err.printStackTrace()
        complete(StatusCodes.InternalServerError)

  extension [A](op: Operation[Route])
    def onError(f: PartialFunction[Error, Route]): Operation[Route] = op andThen {
      case Success(x @ Left(err)) => Success(if (f.isDefinedAt(err)) Right(f(err)) else x)
      case others                 => others
    }

  case class JsonLogResponse(lines: List[String], error: Option[String], tail: Option[String])
  given Encoder[JsonLogResponse] = deriveEncoder

object HttpAdapter:

  extension [A](source: Source[ServerSentEvent, A])(using ExecutionContext)
    def terminateWith(f: () => Unit): Source[ServerSentEvent, Unit] =
      source
        .keepAlive(30.second, () => ServerSentEvent.heartbeat)
        .watchTermination[Unit]()((_, doneFuture) =>
          doneFuture.onComplete(done =>
            done.toEither match
              case Right(_) => f()
              case Left(err) =>
                err.printStackTrace()
                f()
          )
        )

  extension (adapter: HttpAdapter)
    def onError(f: PartialFunction[Error, Route])(using AdapterConfig): HttpAdapter =
      new HttpAdapter {
        override val env = adapter.env
        override def route(resourcePrefix: String): Route =
          val tmp = adapter.errorManager
          adapter.errorManager = error =>
            if (f.isDefinedAt(error)) f(error)
            else tmp(error)
          adapter.route(resourcePrefix)
      }
