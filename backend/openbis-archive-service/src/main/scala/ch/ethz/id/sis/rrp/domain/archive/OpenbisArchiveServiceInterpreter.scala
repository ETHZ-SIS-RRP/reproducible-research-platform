package ch.ethz.id.sis.rrp
package domain
package archive

import ch.ethz.id.sis.rrp.transport.DownloadDataSet
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetTypeFetchOptions
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.IDataSetId
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetTypeSearchCriteria
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.dataset.create.UploadedDataSetCreation
import ch.systemsx.cisd.common.exceptions.InvalidSessionException
import ch.systemsx.cisd.common.http.JettyHttpClientFactory
import org.eclipse.jetty.client.util.MultiPartContentProvider
import org.eclipse.jetty.client.util.PathContentProvider
import org.eclipse.jetty.http.HttpMethod

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList
import scala.jdk.CollectionConverters._

case class OpenbisArchiveServiceInterpreter(openbis: IApplicationServerApi, dss: IDataStoreServerApi)
    extends ArchiveService:
  import OpenbisArchiveServiceInterpreter._

  private def contentOf(item: File): List[File] = {
    if (item.getName().startsWith(".")) {
      List()
    } else if (item.isFile()) {
      List(item)
    } else {
      item.listFiles.foldLeft(List[File]()) { _ ++ contentOf(_) }
    }
  }

  extension (s: String)
    def substr(end: String): String = {
      val index = s.indexOf(end)
      if (index <= 0) {
        s
      } else {
        s.substring(0, index + 1)
      }
    }

  private def getEntityIdentifier(
      sessionToken: String,
      id: String
  ): Operation[Either[ExperimentIdentifier, SampleIdentifier]] =
    sideEffect {
      val experimentSearchCriteria = new ExperimentSearchCriteria()
      experimentSearchCriteria.withIdentifier().thatEquals(id)
      experimentSearchCriteria.withOrOperator().withPermId().thatEquals(id)
      val experiments  = openbis.searchExperiments(sessionToken, experimentSearchCriteria, new ExperimentFetchOptions())
      val experimentId = experiments.getObjects().asScala.headOption.map(_.getIdentifier())

      val sampleSearchCriteria = new SampleSearchCriteria()
      sampleSearchCriteria.withIdentifier().thatEquals(id)
      sampleSearchCriteria.withOrOperator().withPermId().thatEquals(id)
      val samples  = openbis.searchSamples(sessionToken, sampleSearchCriteria, new SampleFetchOptions())
      val sampleId = samples.getObjects().asScala.headOption.map(_.getIdentifier())

      (sampleId, experimentId) match {
        case (Some(sample), _)        => Right(Right(sample))
        case (None, Some(experiment)) => Right(Left(experiment))
        case _                        => Left(Error(s"Could not find an entity with identifier '${id}'"))
      }
    }

  override def listArchiveTypes(session: SessionInfo): Operation[List[ArchiveType]] =
    sideEffect {
      val dstFetch = new DataSetTypeFetchOptions()
      val paFetch  = dstFetch.withPropertyAssignments()
      val ptFetch  = paFetch.withPropertyType()
      ptFetch.withVocabulary()
      ptFetch.withMaterialType()
      ptFetch.withSampleType()

      try {
        val dataSetTypes =
          openbis.searchDataSetTypes(session.openbisToken, new DataSetTypeSearchCriteria(), dstFetch)

        val result = dataSetTypes
          .getObjects()
          .asScala
          .toList
          .map(dst => {
            val ass = dst
              .getPropertyAssignments()
              .asScala
              .map(pa =>
                PropertyAssignment(
                  pa.getPropertyType().getCode(),
                  pa.getPropertyType().getDataType().toString(),
                  pa.isMandatory()
                )
              )
            ArchiveType(dst.getCode(), ass.toList)
          })
        Right(result)
      } catch {
        case _: InvalidSessionException => Left(OpenbisAuthenticationFailed)
      }
    }

  override def archiveProject(
      project: Project,
      dataSetType: String,
      entityId: String,
      properties: Map[String, String],
      session: SessionInfo
  ): Operation[ArchiveId] =
    operation { env =>
      for
        _       <- getEntityIdentifier(session.openbisToken, entityId)
        spec    <- env.processService.archiveProject(session, project, entityId, dataSetType, properties)
        logFile <- env.processService.execute(spec)
      yield logFile
    }

  override def archiveResults(
      project: Project,
      dataSetType: String,
      entityId: String,
      properties: Map[String, String],
      session: SessionInfo
  ): Operation[ArchiveId] =
    operation { env =>
      for
        _       <- getEntityIdentifier(session.openbisToken, entityId)
        spec    <- env.processService.archiveResults(session, project, entityId, dataSetType, properties)
        logFile <- env.processService.execute(spec)
      yield logFile
    }

  override def getArchiveProperties(session: SessionInfo, archiveId: String): Operation[Map[String, String]] =
    sideEffect {
      val fetchOptions = new DataSetFetchOptions()
      fetchOptions.withProperties()
      val ids = new ArrayList[IDataSetId]()
      val id  = new DataSetPermId(archiveId)
      ids.add(id)
      val result  = openbis.getDataSets(session.openbisToken, ids, fetchOptions)
      val dataset = result.get(id)
      val props =
        if (dataset != null) {
          Right(dataset.getProperties().asScala.toMap)
        } else {
          Left(Error("No such archive: " + archiveId))
        }
      props
    }

  override def readFileFromArchive(session: SessionInfo, archiveId: String, path: String): Operation[Option[String]] =
    sideEffect {
      val targetDir  = Files.createTempDirectory("rrp")
      val sourcePath = Path.of(path)
      val dls        = new DownloadDataSet(dss, session.openbisToken, archiveId, sourcePath, targetDir)
      dls.execute()
      readFile(new File(targetDir.toFile(), sourcePath.toString()))
    }

object OpenbisArchiveServiceInterpreter:
  object OpenbisAuthenticationFailed extends Error:
    override val message = "Session expired"
