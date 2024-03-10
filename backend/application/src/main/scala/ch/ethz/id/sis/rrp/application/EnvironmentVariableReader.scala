package ch.ethz.id.sis.rrp
package application

import java.io.File
import java.net.URL

private case class ConfOption(name: String, input: String, actual: String):
  override def toString() =
    if (input == actual)
      s"$name: '$input'"
    else
      s"$name: '$actual' (derived from given value '$input')"

class EnvironmentVariableReader():
  val env = if (sys.env.contains("rrp_export_registry")) sys.env else sys.env + (("rrp_export_registry", ""))
  var options: List[ConfOption] = List()

  def add(option: ConfOption) =
    options = option :: options

  def confReport(): Unit =
    log("-- configuration --")
    options.reverse.foreach(option => log(option.toString))
    log("-------------------")
    log("")

object EnvironmentVariableReader:
  type Conf[A] = String => Either[String, A]

  extension (s: String)(using reader: EnvironmentVariableReader)
    def as[A](conf: Conf[A]): A =
      reader.env.get(s).map(input => (input, conf(input))) match
        case Some((input, Right(value))) =>
          reader.add(ConfOption(s, input, value.toString))
          value
        case Some((input, Left(err))) => throw new IllegalArgumentException(s"$s: $err (given value was $input)")
        case None                     => throw new IllegalArgumentException(s"$s: no value given")

  val integer: Conf[Int] =
    s =>
      try Right(s.toInt)
      catch case _: Throwable => Left("must be integer")

  val real: Conf[Double] =
    s =>
      try Right(s.toDouble)
      catch case _: Throwable => Left("must be a number")

  val prefix: Conf[String] = s => if (s.startsWith("/")) Right(s.substring(1)) else Right(s)

  val file: Conf[File] = s =>
    val f = new File(s)
    if (f.exists()) Right(f) else Left("no such file")

  val such: Conf[String] = s => Right(s)

  val boolean: Conf[Boolean] =
    case "true"  => Right(true)
    case "false" => Right(false)
    case _       => Left("required 'true'or 'false'")

  val url: Conf[URL] = s =>
    try Right(new java.net.URL(s))
    catch case _: Throwable => Left("invalid URL")
