package ch.ethz.id.sis.rrp
package application
package event

import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Source
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter
import io.circe.syntax._

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success

case class ProjectEventStream(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:

  import env._
  import HttpAdapter._

  given Traversable[Set] = setTraversable

  type Changes   = Set[ProjectUpdateEvent]
  type Deletions = Set[String]

  private def getChanges(user: String, resourcePrefix: String): Operation[(Changes, Deletions)] =
    val state = new AtomicReference(Set[ProjectUpdateEvent]())
    for
      projects   <- projectService.getProjectsOf(user)
      detailsRaw <- projects.map(ProjectUpdateEvent(resourcePrefix)).sequence
      details = detailsRaw collect { case Some(value) => value }
      (changed, deleted) = cas(state) { current =>
        val changed = details -- current
        val deleted = current.map(_.id) -- details.map(_.id)
        ((changed, deleted), details)
      }
    yield (changed, deleted)

  private def event(data: String, name: String): ServerSentEvent =
    ServerSentEvent(data, Some("project-update"), Some(name))

  override def route(resourcePrefix: String) =
    (get & path("events")) {
      onValidSession { session =>
        sideEffect {
          val changeTracker = getChanges(session.username, resourcePrefix)
          val source = Source
            .tick(0 seconds, 2 seconds, ())
            .map(_ =>
              changeTracker(env) match
                case Success(Right(value)) => value
                case Success(Left(err)) =>
                  log(err)
                  (Set(), Set())
                case Failure(err) =>
                  err.printStackTrace()
                  (Set(), Set())
            )
            .map((changes, deletions) =>
              val changeEvents =
                changes.map(p => event(List(p).asJson.noSpaces, "update"))
              val deletionEvents = deletions.map(p => event(List(p).asJson.spaces2, "deletion"))
              changeEvents ++ deletionEvents
            )
            .mapConcat(identity)

          complete(source.terminateWith(() => ()))
        }
      }
    }
