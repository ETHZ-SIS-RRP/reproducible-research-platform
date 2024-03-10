package ch.ethz.id.sis.rrp
package application
package logs

import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import akka.stream.scaladsl.Source
import ch.ethz.id.sis.rrp.http.AdapterConfig
import ch.ethz.id.sis.rrp.http.HttpAdapter
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

import java.io.File
import java.util.concurrent.atomic.AtomicReference
import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.util.Try

case class TailSubscription() extends Subscription:

  var subscriber: Subscriber[_ >: ServerSentEvent] = null
  val state                                        = new AtomicReference[Either[Queue[NewLogLine], Int]](Left(Queue()))

  def publish(line: NewLogLine): Unit = {
    handle(cas(state) {
      case Left(q)                      => (List(), Left(q.appended(line)))
      case Right(demand) if demand == 1 => (List(line), Left(Queue()))
      case Right(demand)                => (List(line), Right(demand - 1))
    })
  }

  override def request(count: Long): Unit =
    handle(cas(state) {
      case Left(q) if count <= q.length => (q.take(count.toInt).toList, Left(q.drop(count.toInt)))
      case Left(q)                      => (q.toList, Right(count.toInt - q.length))
      case Right(demand)                => (List(), Right(demand + count.toInt))
    })

  private def handle(events: List[NewLogLine]): Unit = events foreach { line =>
    line.end match {
      case None =>
        subscriber.onNext(ServerSentEvent(line.data, Some("complete")))
      case Some("") => subscriber.onComplete()
      case Some(error) =>
        subscriber.onNext(ServerSentEvent(error, Some("failure")))
        subscriber.onComplete()
    }
  }

  override def cancel(): Unit = ()

case class TailLog(env: ExecutionEnvironment)(using AdapterConfig) extends HttpAdapter:
  import env._

  override def route(resourcePrefix: String) =
    (get & path("projects" / PathMatchers.Segment / "logs" / PathMatchers.Segment / "tail" / PathMatchers.Segment)) {
      (projectId, fileId, startFrom) =>
        onValidSession { session =>
          val startLine = Try(startFrom.toInt).toOption.getOrElse(1)

          val subscription = TailSubscription()
          val publisher = new Publisher[ServerSentEvent]() {

            override def subscribe(s: Subscriber[? >: ServerSentEvent]): Unit =
              subscription.subscriber = s
              s.onSubscribe(subscription)
          }

          val logic =
            for logProcess <- env.logService.tailLog(
                Project(session.username, projectId),
                fileId,
                startLine,
                subscription.publish
              )
            yield complete(
              Source
                .fromPublisher(publisher)
                .keepAlive(30.second, () => ServerSentEvent.heartbeat)
                .watchTermination[Unit]()((_, doneFuture) =>
                  doneFuture.onComplete(done =>
                    done.toEither match
                      case Right(_) => logProcess.close()
                      case Left(err) =>
                        err.printStackTrace()
                        logProcess.close()
                  )
                )
            )

          logic onError { _ => complete(StatusCodes.NotFound) }
        }
    }
