package ch.ethz.id.sis.rrp
package domain
package process

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.dsl.ExecListener
import io.fabric8.kubernetes.client.dsl.ExecListener.Response
import io.fabric8.kubernetes.client.dsl.PodResource

import java.io.ByteArrayOutputStream
import java.util.concurrent.CompletableFuture

case class Listener(data: CompletableFuture[String], baos: ByteArrayOutputStream) extends ExecListener:
  override def onOpen(): Unit = ()
  override def onFailure(t: Throwable, response: Response): Unit =
    t.printStackTrace()
    data.completeExceptionally(t)
  override def onClose(code: Int, reason: String): Unit =
    data.complete(baos.toString())

extension (pod: PodResource[Pod])
  def execute(containerName: String)(cmd: String): Unit =
    val listener = Listener(new CompletableFuture(), new ByteArrayOutputStream())
    pod
      .inContainer(containerName)
      .writingOutput(listener.baos)
      .writingError(listener.baos)
      .usingListener(listener)
      .exec("/bin/sh", "-c", cmd)
