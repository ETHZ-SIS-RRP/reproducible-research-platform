package ch.ethz.id.sis.rrp
package domain
package share

import ch.ethz.id.sis.rrp.infra.ShareRepository
import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID
import scala.io.Source
import scala.util.Failure
import scala.util.Success
import scala.util.Using

case class ShareServiceInterpreter(repository: ShareRepository) extends ShareService:

  override def createShareId(project: Project, description: String): Operation[String] =
    operation { env =>
      for
        details <- env.cloneService.getDetailsOf(project)
        _ <- failIf(!details.clean)("Cannot create a share identifier from repository that has uncommited changes")
        _ <- failIf(!details.remoteTracking.isDefined)("Branch not tracked")
        _ <- failIf(details.remoteTracking.map(rt => rt.ahead + rt.behind).getOrElse(0) != 0)(
          details.remoteTracking.toString()
        )
        id <- repository.createShare(
          project,
          repository.PersistedShare(details.remote.get, details.commit.hash, LocalDateTime.now(), description)
        )
      yield id
    }

  override def listSharesOf(project: Project): Operation[List[Share]] =
    given Traversable[Set] = setTraversable
    operation { env =>
      for
        projectName <- env.projectService.getNameOf(project)
        shareData   <- repository.listShares(project)
        commits     <- env.cloneService.getCommits(project, shareData.map(_._2.commitHash))
        result = shareData.foldLeft(List[Share]()) { case (acc, (shareId, persistedShare)) =>
          val dummyCommit = CommitDetails(persistedShare.commitHash, "", LocalDateTime.now(), "unknown", "unknown")
          val commit      = commits.get(persistedShare.commitHash)
          val commitValue = commit.getOrElse(dummyCommit)

          Share(
            shareId,
            project.user,
            project.id,
            projectName,
            persistedShare.repository,
            persistedShare.createdAt,
            persistedShare.description,
            commitValue,
            commit match {
              case Some(_) => None
              case None =>
                Some(s"Commit ${persistedShare.commitHash.substring(0, 7)} has been removed from the repository")
            }
          ) :: acc
        }
      yield result
    }

  override def getShare(shareId: String): Operation[Share] =
    operation { env =>
      for
        project      <- repository.readProjectOf(shareId)
        projectName  <- env.projectService.getNameOf(project)
        shareDataOpt <- repository.readShare(shareId)
        _            <- failIf(!shareDataOpt.isDefined)(ShareNotFound)
        shareData = shareDataOpt.get
        commit <- env.cloneService.getCommit(project, shareData.commitHash)
        _ <- failIf(commit.isEmpty)(s"Commit ${shareData.commitHash} does not exist in repository of project $project")
      yield Share(
        shareId,
        project.user,
        project.id,
        projectName,
        shareData.repository,
        shareData.createdAt,
        shareData.description,
        commit.get,
        None
      )
    }

  override def deleteShare(project: Project, shareId: String): Operation[Unit] =
    repository.deleteShare(project, shareId)
