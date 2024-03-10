package ch.ethz.id.sis.rrp
package domain
package clone

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.errors.NotSupportedException
import org.eclipse.jgit.lib.BranchTrackingStatus
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.submodule.SubmoduleWalk
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

import java.io.File
import java.io.FileOutputStream
import java.io.FilterWriter
import java.io.PrintWriter
import java.io.Writer
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import scala.jdk.CollectionConverters._
import scala.util.Failure
import scala.util.Success
import scala.util.Using

class JGitClient(actorSystem: ActorSystem):

  val gitlab = GitLabExtension(actorSystem)

  private def gitClone(
      url: String,
      repoDir: File,
      credentials: Option[Credentials],
      logFile: ProcessLog,
      writer: Writer
  ): Unit = {

    val (username, password) =
      credentials match {
        case Some(Credentials(username, password)) => (username, password)
        case None                                  => ("", "")
      }

    writer.write(s"\rCloning ${url}\n")
    writer.flush()
    try {
      val git = Git
        .cloneRepository()
        .setURI(url.toString())
        .setCloneAllBranches(true)
        .setProgressMonitor(new TextProgressMonitor(writer))
        .setDirectory(repoDir)
        .setTimeout(5)
        .setCredentialsProvider(
          new UsernamePasswordCredentialsProvider(username, password)
        )
        .setCloneSubmodules(true)
        .call()

      writer.close()
      logFile.closeWithSuccess()

      val config = git.getRepository().getConfig()

      config.setString("credential", null, "helper", "cache --timeout=86400")

      gitlab.getUserInfo(url, credentials) match {
        case Some((name, email)) => {
          name.foreach(config.setString("user", null, "name", _))
          email.foreach(config.setString("user", null, "email", _))
        }
        case _ => ()
      }

      config.save()

      val repo          = git.getRepository()
      val gitmodulesCfg = new FileBasedConfig(new File(repo.getWorkTree(), Constants.DOT_GIT_MODULES), repo.getFS());
      gitmodulesCfg.load()
      val mw = new SubmoduleWalk(repo);
      mw.setModulesConfig(gitmodulesCfg);
      val generator = SubmoduleWalk.forIndex(repo);

      while (generator.next())
        val cfg  = generator.getRepository().getConfig()
        val name = cfg.getString("core", null, "worktree").split("/").reverse.head
        cfg.setString("core", null, "worktree", "../../../" + name)
        cfg.save()
        writeFile(new File(generator.getDirectory(), ".git"), "gitdir: ../.git/modules/" + name + "\n")

      generator.close()
      git.close()

    } catch {
      case _: InvalidRemoteException =>
        logFile.closeWithError(s"Invalid remote $url")
      case _: UnknownHostException =>
        logFile.closeWithError(s"Invalid remote $url")
      case t: NotSupportedException =>
        t.printStackTrace()
        logFile.closeWithError(s"No such repository $url")
      case t: Throwable => {
        t.printStackTrace()
        logFile.closeWithError(rootMessageOf(t))
      }
    }
  }

  def clone(url: String, target: File, logFile: File, credentials: Option[Credentials]): Operation[Unit] =
    target.mkdirs()
    val log = ProcessLog(logFile)

    val writer = new FilterWriter(new PrintWriter(new FileOutputStream(log.file))) {
      override def write(s: String): Unit =
        super.write(s.replaceAll("\r", "\r\n"))
    }

    actorSystem.dispatcher.execute(new Runnable {
      override def run(): Unit = gitClone(url, target, credentials, log, writer)
    })
    Right(())

  def checkout(cloneDir: File, pointer: String): Operation[Unit] = sideEffect {
    Using.resource(Git.open(cloneDir)) { git =>
      val allRefs = git.getRepository().getRefDatabase().getRefs()
      val revWalk = new RevWalk(git.getRepository())

      var tags = List[String]()
      allRefs.asScala.foreach(ref =>
        revWalk.markStart(revWalk.parseCommit(ref.getObjectId()))
        if (ref.getName().startsWith("refs/tags/"))
          tags = ref.getName().substring(10) :: tags
      )

      var commits = List[String]()
      revWalk.asScala.foreach(commit => commits = commit.getName() :: commits)

      val branch   = if (pointer.isBlank()) "main" else pointer
      val isCommit = branch.length >= 7 && commits.find(_.startsWith(branch)).isDefined
      val isTag    = tags.contains(branch)

      if (isCommit || isTag) {
        git
          .checkout()
          .setName(branch)
          .call()
        Right(())
      } else {
        try {
          git
            .checkout()
            .setName(branch)
            .setCreateBranch(!(branch.equals("master") || branch.equals("main")))
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            .setStartPoint("origin/" + branch)
            .call()
          Right(())
        } catch {
          case _: Throwable => Left(Error(s"No such branch / tag / commit: $branch"))
        }
      }
    }
  }

  def getDetailsOf(cloneDir: File): Operation[CloneDetails] = sideEffect {
    Using.resource(Git.open(cloneDir)) { git =>
      val remoteRaw = git.getRepository().getConfig().getString("remote", "origin", "url")
      val head      = git.log().call().asScala.head

      val details =
        CloneDetails(
          remote = if (remoteRaw != null) Some(remoteRaw) else None,
          commit = CommitDetails(
            hash = head.name(),
            message = head.getFullMessage().trim(),
            timestamp =
              LocalDateTime.ofInstant(new Date(head.getCommitTime().toLong * 1000).toInstant(), ZoneId.systemDefault()),
            authorName = head.getAuthorIdent().getName(),
            authorEmail = head.getAuthorIdent().getEmailAddress()
          ),
          clean = git.status().call().isClean(),
          branch = git.getRepository().getBranch(),
          remoteTracking = BranchTrackingStatus.of(git.getRepository(), git.getRepository().getBranch()) match
            case null => None
            case status =>
              Some(
                RemoteTracking(
                  branch = status.getRemoteTrackingBranch(),
                  ahead = status.getAheadCount(),
                  behind = status.getBehindCount()
                )
              )
        )
      Right(details)
    }
  }

  def getCommits(cloneDir: File, hashes: Iterable[String]): Operation[Map[String, CommitDetails]] =
    sideEffect {
      Using.resource(Git.open(cloneDir)) { git =>
        val repo    = git.getRepository()
        val revWalk = new RevWalk(repo)
        val commits = hashes.map(repo.resolve).filter(_ != null).map(revWalk.parseCommit)
        commits.foldLeft(Map[String, CommitDetails]()) { (acc, commit) =>
          val hash   = commit.getName()
          val author = commit.getAuthorIdent()
          val details = CommitDetails(
            hash,
            commit.getFullMessage(),
            LocalDateTime.ofInstant(new Date(commit.getCommitTime().toLong * 1000).toInstant(), ZoneId.systemDefault()),
            author.getName(),
            author.getEmailAddress()
          )
          acc + ((hash, details))
        }
      }
    }
