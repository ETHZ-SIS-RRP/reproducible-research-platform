package ch.ethz.id.sis.rrp
package infra
package bundle

import ch.ethz.id.sis.rrp.json.given
import io.circe.Codec
import io.circe.generic.semiauto._
import io.circe.syntax._

import java.io.File
import java.time.LocalDateTime
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Using

case class FileSystemBundleRepository(bundleDir: Project => File, logDir: Project => File) extends BundleRepository:

  given Codec[OfflineBundle] = deriveCodec

  def createBundle(project: Project, description: String, commit: CommitDetails): Operation[OfflineBundle] =
    sideEffect {
      val now       = LocalDateTime.now()
      val nowString = now.format(formatterNoSpaces)
      val name      = s"${nowString}_${project.id}-player-bundle"
      val logFile   = new File(logDir(project), s"${nowString}_create-offline-bundle.log")
      val bundle = OfflineBundle(
        project,
        name,
        description,
        now,
        commit
      )
      writeFile(new File(bundleDir(project), s"${name}.json"), bundle.asJson.spaces2)
      bundle
    }

  private def getBundleStatus(bundle: OfflineBundle): BundleStatus = {
    val bundleFile = new File(bundleDir(bundle.project), bundle.name + ".zip")
    val finishFile = new File(logDir(bundle.project), bundle.name + ".log.rrp")
    val finished   = finishFile.exists()
    val success    = finished && finishFile.length == 0
    BundleStatus(success, finished, if (bundleFile.exists()) bundleFile.length() else 0)
  }

  def listBundles(project: Project): Operation[List[(OfflineBundle, BundleStatus)]] = sideEffect {
    val files = bundleDir(project).listFiles().toList.filter(_.isFile()).filter(_.getName().endsWith(".json"))
    val (_, ok) = files
      .map(parseBundleFrom)
      .partitionMap({
        case Right(value) => Right(value)
        case Left(err) =>
          log(s"Error when listing bundles: ${err.message}")
          Left(())
      })
    ok.map(bundle => (bundle, getBundleStatus(bundle)))
  }

  private def parseBundleFrom(file: File): Either[Error, OfflineBundle] =
    if (file.exists())
      Using(Source.fromFile(file)(using scala.io.Codec.UTF8)) { content =>
        io.circe.parser
          .decode[OfflineBundle](content.mkString)
          .left
          .map(err => s"${file.getAbsolutePath()}: ${err.getMessage()}")
      } match {
        case Success(ok) => ok.left.map(Error(_))
        case Failure(err) =>
          err.printStackTrace()
          Left(Error(err.getMessage()))
      }
    else
      Left(Error(s"File ${file.getAbsolutePath} does not exist"))

  def deleteBundle(project: Project, fileName: String): Operation[Unit] =
    sideEffect {
      new File(bundleDir(project), fileName).delete()
      new File(bundleDir(project), fileName.replace(".zip", ".json")).delete()
    }
