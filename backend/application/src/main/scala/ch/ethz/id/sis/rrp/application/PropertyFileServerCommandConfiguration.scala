package ch.ethz.id.sis.rrp
package application

import java.util.Properties
import scala.io.Source

case class PropertyFileServerCommandConfiguration(filename: String) extends ServerCommandConfiguration:

  val launch = new Properties()
  launch.load(Source.fromResource(filename).bufferedReader())

  override def apply(image: Image, runtime: String)(project: Project): Operation[String] =
    sideEffect {
      val prop = launch.getProperty(image.buildToolName + "/" + image.buildToolVersion + "/" + runtime)
      if (prop == null) Left(Error(s"Cannot find command for $image"))
      else Right(prop.replace("${p.id}", project.id))
    }
