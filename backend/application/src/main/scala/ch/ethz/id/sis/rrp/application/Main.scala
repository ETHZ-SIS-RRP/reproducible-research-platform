package ch.ethz.id.sis.rrp
package application

object Main:
  def main(args: Array[String]): Unit =
    val service = new HttpServiceBuilder(args).build()
    service.run()
