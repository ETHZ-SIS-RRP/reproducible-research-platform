package ch.ethz.id.sis.rrp
package domain
package process

import io.fabric8.kubernetes.client.KubernetesClient
import java.util.concurrent.atomic.AtomicReference
import scala.jdk.CollectionConverters._
import io.fabric8.kubernetes.api.model.PodListBuilder
import io.fabric8.kubernetes.api.model.PodStatus

class PodMonitor(client: KubernetesClient, namespace: String) extends Runnable:

  private val state = new AtomicReference(Map[Project, Set[ProcessInfo]]())

  def processesOf(project: Project): Set[ProcessInfo] = state.get().getOrElse(project, Set())

  private def checkStatus(podStatus: PodStatus): Boolean =
    val healthy =
      for
        status <- Option(podStatus)
        if Option(status.getPhase()).contains("Running")
        statuses <- Option(status.getContainerStatuses()).map(_.asScala.toList)
        if statuses.nonEmpty
        containersReady = statuses.forall(cs => Option(cs.getReady()).contains(true))
      yield containersReady

    healthy.getOrElse(false)

  override def run(): Unit =
    try
      val pods =
        client
          .pods()
          .inNamespace(namespace)
          .list()
          .getItems()
          .asScala
          .toList

      val containerMap =
        pods
          .map(pod => (pod.getMetadata().getName(), pod.getStatus()))
          .foldLeft(Map[Project, Set[ProcessInfo]]()) { case (acc, (name, status)) =>
            val tokens = name.split("-", 3)
            if (tokens.length >= 3)
              val (podType, id, user) = (tokens(0), tokens(1), tokens(2))

              val typeMap = Map(
                "build"     -> Build,
                "server"    -> Server,
                "archive"   -> Archive,
                "unarchive" -> Unarchive,
                "offline"   -> OfflineBundleProcess
              )

              typeMap.get(podType) match
                case Some(processType) =>
                  acc |+| Map(Project(user, id) -> Set(ProcessInfo(processType, checkStatus(status), name)))
                case None => acc
            else acc
          }

      state.set(containerMap)

      val completed =
        pods
          .filter(
            _.getStatus()
              .getContainerStatuses()
              .asScala
              .toList
              .map(_.getState().getTerminated() != null)
              .contains(true)
          )

      if (!completed.isEmpty)
        log(s"Deleting completed pods ${completed.map(_.getMetadata().getName()).mkString(", ")}")
        client.resourceList(new PodListBuilder().withItems(completed.asJava).build()).withGracePeriod(0).delete()
    catch case t: Throwable => t.printStackTrace()
  end run
