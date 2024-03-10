package ch.ethz.id.sis.rrp

import org.apache.commons.io.FileUtils
import org.apache.commons.io.input.Tailer
import org.apache.commons.io.input.TailerListenerAdapter
import org.apache.commons.lang3.RandomStringUtils

import java.io.Closeable
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.Scanner
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import scala.io.Source
import scala.util.NotGiven
import scala.util.Try

def rootMessageOf(t: Throwable): String =
  var cause = t
  var msg   = ""
  while (cause != null)
    var newMsg = cause.getMessage()
    if (newMsg != null) {
      newMsg = newMsg.trim()
      if (!newMsg.isBlank()) msg = newMsg
    }
    cause = cause.getCause()
  end while
  msg

def throwableToString(t: Throwable): String =
  if t != null then
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    t.printStackTrace(pw)
    sw.toString
  else "no exception"

type IdCreator = () => String
val uid: IdCreator = () => RandomStringUtils.randomAlphanumeric(8).toLowerCase(Locale.ROOT)

val formatter         = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
val formatterNoSpaces = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

def log(s: String): Unit =
  val now = LocalDateTime.now().format(formatter)
  println(s"$now\t$s")

def log(e: Error): Unit =
  log(e.message)

def debug(s: String)(using pw: PrintWriter): Unit =
  try
    val now = LocalDateTime.now().format(formatter)
    synchronized {
      pw.println(s"$now\t$s")
      pw.flush()
    }
  catch
    case t: Throwable =>
      log("----")
      log("Debugging failure")
      log(throwableToString(t))
      log(s)
      log("----")

def debug(e: Error)(using pw: PrintWriter): Unit =
  debug(e.message)

def debug(t: Throwable)(using pw: PrintWriter): Unit =
  debug(throwableToString(t))

def cas[A, R](ref: AtomicReference[A])(next: A => (R, A)): R =
  while true do
    val expect              = ref.get
    val (returned, updated) = next(expect)
    if ref.compareAndSet(expect, updated) then return returned
  throw new AssertionError

def operation[A](f: ExecutionEnvironment => Operation[A]): Operation[A] = env => f(env)(env)

def sideEffect[A](a: => A)(using NotGiven[A <:< Either[Error, _]]): Operation[A] = env => Try { Right(a) }
def sideEffect[A](a: => Either[Error, A]): Operation[A]                          = env => Try { a }
def unit[A](a: => A): Operation[A]                                               = env => Try { Right(a) }

def failIf(condition: Boolean)(msg: String): Operation[Unit] =
  env => Try { if (condition) Left(Error(msg)) else Right(()) }

def failIf(condition: Boolean)(error: Error): Operation[Unit] =
  env => Try { if (condition) Left(error) else Right(()) }

def readFile(file: File): Option[String] =
  if (file.exists())
    Some(FileUtils.readFileToString(file, Charset.defaultCharset()))
  else
    None

def writeFile(file: File, value: String): Unit = Files.write(
  file.toPath(),
  value.getBytes(StandardCharsets.UTF_8)
)

given Conversion[Date, LocalDateTime] with
  def apply(date: Date): LocalDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
