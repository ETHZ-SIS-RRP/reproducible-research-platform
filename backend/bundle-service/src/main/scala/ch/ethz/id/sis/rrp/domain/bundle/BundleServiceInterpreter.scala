package ch.ethz.id.sis.rrp
package domain
package bundle

import ch.ethz.id.sis.rrp.infra.BundleRepository

case class BundleServiceInterpreter(repository: BundleRepository) extends BundleService:

  override def createBundle(project: Project, description: String): Operation[OfflineBundle] = operation { env =>
    for
      cloneDetails <- env.cloneService.getDetailsOf(project)
      bundle       <- repository.createBundle(project, description, cloneDetails.commit)
    yield bundle
  }

  override def listBundlesOf(project: Project): Operation[List[(OfflineBundle, BundleStatus)]] =
    repository.listBundles(project)

  override def deleteBundle(project: Project, fileName: String): Operation[Unit] =
    repository.deleteBundle(project, fileName)
