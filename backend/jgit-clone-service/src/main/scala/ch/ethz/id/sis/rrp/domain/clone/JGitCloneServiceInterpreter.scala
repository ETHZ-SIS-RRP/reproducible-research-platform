package ch.ethz.id.sis.rrp
package domain
package clone

import akka.actor.ActorSystem
import org.apache.commons.io.FileUtils

import java.io.File
import scala.util.Using

case class JGitCloneServiceInterpreter(actorSystem: ActorSystem, logDir: Project => File) extends CloneService:
  override opaque type Remote = ch.ethz.id.sis.rrp.domain.clone.Remote

  override val cloneLogFileName = "clone.log"

  val gitClient = new JGitClient(actorSystem)

  override def createRemote(url: String) = operationMonad.unit(Remote(url))

  override def createClone(project: Project, remote: Remote, credentials: Option[Credentials]) =
    operation { env =>
      for
        repositoryDir <- env.projectService.getCloneDirOf(project)
        _ <- gitClient.clone(
          remote.url,
          repositoryDir,
          new File(logDir(project), cloneLogFileName),
          credentials
        )
      yield ()
    }
  override def checkout(project: Project, pointer: String): Operation[Unit] =
    operation { env =>
      for
        repoDir <- env.projectService.getCloneDirOf(project)
        _       <- gitClient.checkout(repoDir, pointer)
      yield ()
    }

  override def getDetailsOf(project: Project): Operation[CloneDetails] =
    operation { env =>
      for
        repoDir <- env.projectService.getCloneDirOf(project)
        details <- gitClient.getDetailsOf(repoDir)
      yield details
    }

  override def getCommits(project: Project, hashes: Iterable[Hash]): Operation[Map[Hash, CommitDetails]] =
    operation { env =>
      for
        repoDir <- env.projectService.getCloneDirOf(project)
        result  <- gitClient.getCommits(repoDir, hashes)
      yield result
    }
