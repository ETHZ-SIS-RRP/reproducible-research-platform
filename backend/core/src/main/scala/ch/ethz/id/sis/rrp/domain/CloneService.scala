package ch.ethz.id.sis.rrp
package domain

import java.io.File

trait CloneService:
  type Remote
  type Hash = String
  val cloneLogFileName: String

  def createRemote(url: String): Operation[Remote]
  def createClone(project: Project, remote: Remote, credentials: Option[Credentials]): Operation[Unit]
  def checkout(project: Project, pointer: String): Operation[Unit]
  def getDetailsOf(project: Project): Operation[CloneDetails]
  def getCommits(project: Project, hashes: Iterable[Hash]): Operation[Map[Hash, CommitDetails]]
  def getCommit(project: Project, hash: Hash): Operation[Option[CommitDetails]] =
    getCommits(project, List(hash)).map(_.get(hash))
