package ch.ethz.id.sis.rrp
package domain
package mount

import ch.ethz.id.sis.rrp.domain.mount.MappingReader.readMapping
import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.search.DataSetSearchCriteria
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria
import org.apache.commons.io.FileUtils

import java.io.File
import scala.jdk.CollectionConverters._

case class OpenbisMountingServiceInterpreter(v3api: IApplicationServerApi, testResearchCollection: Boolean)
    extends MountingService:

  def readMountConfigOf(project: Project): Operation[String] =
    operation { env =>
      for
        repoDir     <- env.projectService.getCloneDirOf(project)
        fileContent <- sideEffect { readFile(new File(repoDir, ".rrp/datasets.yaml")) }
      yield fileContent.getOrElse("")
    }

  def refreshMountsOf(project: Project, session: SessionInfo): Operation[Unit] = {
    operation { env =>
      for
        repoDir     <- env.projectService.getCloneDirOf(project)
        linkDir     <- env.projectService.getLinksDirOf(project)
        runDir      <- env.projectService.getRunDirOf(project)
        fileContent <- sideEffect { readFile(new File(repoDir, ".rrp/datasets.yaml")) }
        _ <- sideEffect {
          linkDir
            .listFiles()
            .foreach(f => if (f.isDirectory()) FileUtils.deleteDirectory(f) else f.delete())

          fileContent match {
            case Some(yaml) =>
              try
                val mapping = MappingReader.readMapping(yaml)
                readFile(new File(runDir, "uuid")) foreach { uuid =>
                  val linker = new DatasetLinker(v3api, session.openbisToken, s"/mnt/${uuid}/openbis/DEFAULT")
                  linker.createLinks(linkDir, mapping)
                }
              catch
                case m: MappingException =>
                  log(s"Project $project has invalid dataset mapping")
                  m.printStackTrace()
            case None => ()
          }
        }
      yield ()
    }
  }

  def updateMountConfigOf(project: Project, spec: String, session: SessionInfo): Operation[Unit] =
    operation { env =>
      for
        mapping <- sideEffect {
          try Right(MappingReader.readMapping(spec))
          catch case m: MappingException => Left(Error(m.getMessage()))
        }
        repoDir <- env.projectService.getCloneDirOf(project)
        _ <- sideEffect {
          val rrpDir = new File(repoDir, ".rrp")
          rrpDir.mkdirs()
          writeFile(new File(rrpDir, "datasets.yaml"), spec)
        }
        _ <- refreshMountsOf(project, session)
      yield ()
    }

  private def readMapping(project: Project): Operation[Map[String, String]] =
    for
      spec <- readMountConfigOf(project)
      mapping <- sideEffect {
        try Right(MappingReader.readMapping(spec).destinations.asScala.toMap.transform((k, v) => v.asScala.toList))
        catch case m: MappingException => Left(Error(m.getMessage()))
      }
    yield mapping.foldLeft(Map[String, String]()) { case (acc, (path, ids)) =>
      ids.foldLeft(acc) { (result, id) => result + ((path + "/" + id, id)) }
    }

  private def getExportUrls(datasetIds: List[String], openbisToken: String): Operation[Map[String, Option[String]]] =
    sideEffect {
      val searchCriteria = new DataSetSearchCriteria().withOrOperator()
      datasetIds.foreach(id => searchCriteria.withPermId().thatEquals(id))

      val fetchOptions = new DataSetFetchOptions()
      fetchOptions.withContainers().withSample()
      fetchOptions.withContainers().withSample().withType()
      fetchOptions.withContainers().withSample().withProperties()

      val results =
        v3api.searchDataSets(openbisToken, searchCriteria, fetchOptions).getObjects().asScala.toList

      val data = results.foldLeft(Map[String, Option[String]]()) { (acc, dataset) =>
        val id = dataset.getPermId().getPermId()

        val container = dataset
          .getContainers()
          .asScala
          .find(container => container.getSample().getType().getCode().equals(("PUBLICATION")))

        val url = container.map(c => c.getSample().getProperties().get("$PUBLICATION.URL"))
        acc + ((id, url))
      }
      Right(datasetIds.foldLeft(data) { (acc, id) => if (!acc.contains(id)) acc + ((id, None)) else acc })
    }

  private def patchUrls(mapping: Map[String, Option[String]]): Operation[Map[String, Option[String]]] =
    unit {
      mapping.transform((id, urlOpt) =>
        urlOpt.map(url =>
          if (url.contains("10.5072/zenodo")) {
            val id = url.split('.').last
            s"https://sandbox.zenodo.org/record/$id/files/content.zip"
          } else if (url.contains("zenodo")) {
            val id = url.split('.').last
            s"https://zenodo.org/record/$id/files/content.zip"
          } else if (url.contains("hdl.handle.net")) {
            val server =
              if (testResearchCollection) "https://test.research-collection.ethz.ch/"
              else "https://www.research-collection.ethz.ch/"
            server + "bitstream/handle/" + url.split("handle.net/").last + "/content.zip"
          } else {
            url
          }
        )
      )
    }

  def readExportStatusOf(project: Project, sessionInfo: SessionInfo): Operation[Map[String, (String, Option[String])]] =
    for
      mapping <- readMapping(project)
      datasetIds = mapping.values.toSet.toList
      exportUrls     <- getExportUrls(datasetIds, sessionInfo.openbisToken)
      patchedMapping <- patchUrls(exportUrls)
    yield mapping.transform((k, v) => (v, patchedMapping.get(v).flatten))
