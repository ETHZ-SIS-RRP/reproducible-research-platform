package ch.ethz.id.sis.rrp

trait ServerCommandConfiguration:
  def apply(image: Image, runtime: String)(project: Project): Operation[String]
