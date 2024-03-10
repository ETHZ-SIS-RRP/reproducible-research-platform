package ch.ethz.id.sis.rrp
package infra
package share

import io.circe.Codec
import io.circe.generic.semiauto._
import io.circe.syntax._

import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Using

case class FileSystemShareRepository(getDir: Project => File) extends ShareRepository:

  given Codec[PersistedShare] = deriveCodec

  private def shareDirOf(project: Project): File =
    val dir = getDir(project)
    dir.mkdirs()
    dir

  def createShare(project: Project, data: PersistedShare): Operation[String] =
    sideEffect {
      val idPlain = UUID.randomUUID().toString() + " " + project.user + " " + project.id + " " + uid()
      val id      = Base64.getEncoder.encodeToString(idPlain.getBytes(StandardCharsets.UTF_8))
      val json    = data.asJson.spaces2
      writeFile(new File(shareDirOf(project), id), json)
      id
    }

  def readShare(shareId: String): Operation[Option[PersistedShare]] =
    for
      project <- readProjectOf(shareId)
      result <- sideEffect {
        val shareFile = new File(getDir(project), shareId)
        parseShareFrom(shareFile)
      }
    yield result

  def listShares(project: Project): Operation[List[(ShareId, PersistedShare)]] =
    sideEffect {
      val shareDir = shareDirOf(project)
      val files    = shareDir.listFiles().toList.filter(_.isFile())
      val parsed   = files.map(file => parseShareFrom(file).map(_.map((file.getName(), _))))

      val (_, ok) = parsed.partitionMap({
        case Right(value) => Right(value)
        case Left(err) =>
          log(s"Error when listing shares: $err")
          Left(())
      })
      ok.flatten
    }

  private def parseShareFrom(file: File): Either[Error, Option[PersistedShare]] =
    if (file.exists())
      Using(Source.fromFile(file)(using scala.io.Codec.UTF8)) { content =>
        io.circe.parser
          .decode[PersistedShare](content.mkString)
          .left
          .map(err => s"${file.getAbsolutePath()}: ${err.getMessage()}")
      } match {
        case Success(ok) => ok.map(Some(_)).left.map(Error(_))
        case Failure(err) =>
          err.printStackTrace()
          Left(Error(err.getMessage()))
      }
    else
      Right(None)

  def deleteShare(project: Project, shareId: String): Operation[Unit] =
    sideEffect {
      val shareFile = new File(shareDirOf(project), shareId)
      shareFile.delete()
    }

  override def readProjectOf(shareId: String): Operation[Project] =
    sideEffect {
      try
        val plainText = new String(Base64.getDecoder().decode(shareId)).split(" ")
        val username  = plainText(1)
        val projectId = plainText(2)
        Right(Project(username, projectId))
      catch case t: Throwable => Left(Error("Invalid share id"))
    }
