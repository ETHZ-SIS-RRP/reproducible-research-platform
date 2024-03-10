package ch.ethz.id.sis.rrp
package domain
package process

import ch.ethz.id.sis.rrp.ExecutionEnvironment
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.dsl.PodResource

import java.io.ByteArrayInputStream
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters._

case class KubernetesProcessServiceInterpreter(
    namespace: String,
    subpath: File => String,
    logDir: Project => File
) extends ProcessService:

  case class Spec[A](project: Project, yaml: String, value: A, postOperation: Operation[Unit])

  private val client  = new DefaultKubernetesClient()
  private val ex      = new ScheduledThreadPoolExecutor(3)
  private val monitor = new PodMonitor(client, namespace)
  ex.scheduleAtFixedRate(monitor, 0, 2, TimeUnit.SECONDS)

  private def externalImageName(env: ExecutionEnvironment)(name: String): String =
    s"${env.externalRegistry}/$name:${env.tag}"

  override def buildProjectFromGit(project: Project): Operation[Spec[Image]] =
    operation { env =>
      for
        cloneDir <- env.projectService.getCloneDirOf(project)
        runDir   <- env.projectService.getRunDirOf(project)
        imageName = s"${project.user}/${project.id}-${project.user}"
        yaml = new BuildFromGitPodConfig(
          s"build-${project.id}-${project.user}",
          env.internalRegistry,
          s"${env.internalRegistry}/$imageName",
          namespace,
          subpath(cloneDir),
          subpath(logDir(project)),
          externalImageName(env)
        ).toYaml()
      yield Spec(
        project,
        yaml,
        Image(
          imageName,
          "repo2docker",
          env.buildInfo.getProperty("repo2docker.version")
        ),
        sideEffect { writeFile(new File(runDir, "repo2docker.yaml"), yaml) }
      )
    }

  override def startServer(
      session: SessionInfo,
      project: Project
  ): Operation[Spec[Unit]] =
    operation { env =>
      for
        cloneDir      <- env.projectService.getCloneDirOf(project)
        resultsDir    <- env.projectService.getResultsDirOf(project)
        linksDir      <- env.projectService.getLinksDirOf(project)
        runDir        <- env.projectService.getRunDirOf(project)
        image         <- env.projectService.getImageOf(project)
        memoryRequest <- env.projectService.getMemoryRequestOf(project)
        cpuRequest    <- env.projectService.getCpuRequestOf(project)
        command       <- env.serverCommandConfiguration(image, "kubernetes")(project)
        runId     = java.util.UUID.randomUUID().toString()
        podName   = s"server-${project.id}-${project.user}"
        timestamp = LocalDateTime.now().format(formatterNoSpaces)
        logFile   = timestamp + "_server.log"
        yaml = new ServerPodConfig(
          podName,
          logFile,
          project.id,
          env.namespace,
          s"${env.internalRegistry}/${image.plainName}",
          command,
          runId,
          env.openbis,
          session.openbisToken,
          env.sshHost,
          env.sshPort,
          memoryRequest,
          cpuRequest,
          subpath(cloneDir),
          subpath(resultsDir),
          subpath(logDir(project)),
          subpath(linksDir),
          externalImageName(env)
        ).toYaml()
      yield Spec(
        project,
        yaml,
        (),
        for
          _ <- sideEffect {
            writeFile(new File(runDir, "uuid"), runId)
            writeFile(new File(runDir, timestamp + "_server.yaml"), yaml)
            writeFile(new File(logDir(project), logFile), "Starting server...\n")

            ex.execute(new Runnable {
              override def run =
                try
                  val pod =
                    client
                      .pods()
                      .inNamespace(namespace)
                      .withName(podName)

                  pod.waitUntilReady(120, TimeUnit.SECONDS)
                  pod.execute("main")("chmod -R -w /home/jovyan/build")
                catch case t: Throwable => t.printStackTrace()
            })
          }
          _ <- env.mountingService.refreshMountsOf(project, session)
        yield ()
      )
    }

  override def archiveProject(
      session: SessionInfo,
      project: Project,
      entityId: String,
      datasetType: String,
      properties: Map[String, String]
  ): Operation[Spec[String]] = operation { env =>
    for
      cloneDir <- env.projectService.getCloneDirOf(project)
      runDir   <- env.projectService.getRunDirOf(project)
      image    <- env.projectService.getImageOf(project)
      timestamp = LocalDateTime.now().format(formatterNoSpaces)
      logFile   = timestamp + "_archive-project.log"
      yaml = new ArchiveProjectPodConfig(
        s"archive-${project.id}-${project.user}",
        logFile,
        env.namespace,
        session.openbisToken,
        subpath(cloneDir),
        subpath(logDir(project)),
        externalImageName(env),
        env.internalRegistry,
        image.plainName,
        image.buildToolVersion,
        env.openbis.toString(),
        env.dss.toString(),
        entityId,
        datasetType,
        properties.asJava
      ).toYaml()
    yield Spec(
      project,
      yaml,
      logFile,
      sideEffect {
        writeFile(new File(logDir(project), logFile), "Starting project archive...\n")
        writeFile(new File(runDir, timestamp + "_archive-project.yaml"), yaml)
      }
    )
  }

  override def archiveResults(
      session: SessionInfo,
      project: Project,
      entityId: String,
      datasetType: String,
      properties: Map[String, String]
  ): Operation[Spec[String]] = operation { env =>
    for
      resultsDir <- env.projectService.getResultsDirOf(project)
      runDir     <- env.projectService.getRunDirOf(project)
      timestamp = LocalDateTime.now().format(formatterNoSpaces)
      logFile   = timestamp + "_archive-results.log"
      yaml = new ArchiveResultsPodConfig(
        s"archive-${project.id}-${project.user}",
        logFile,
        env.namespace,
        session.openbisToken,
        subpath(resultsDir),
        subpath(logDir(project)),
        externalImageName(env),
        env.openbis.toString(),
        env.dss.toString(),
        entityId,
        datasetType,
        properties.asJava
      ).toYaml()
    yield Spec(
      project,
      yaml,
      logFile,
      sideEffect {
        writeFile(new File(logDir(project), logFile), "Starting archiving results...\n")
        writeFile(new File(runDir, timestamp + "_archive-results.yaml"), yaml)
      }
    )
  }

  override def buildProjectFromArchive(
      session: SessionInfo,
      project: Project,
      datasetId: String
  ): Operation[Spec[(ProjectName, Image)]] = operation { env =>
    for
      repo2DockerVersion <- env.archiveService
        .readFileFromArchive(session, datasetId, "original/DEFAULT/repo2docker-version")
        .map(_.get)
        .map(_.trim())
      imageName <- env.archiveService
        .readFileFromArchive(session, datasetId, "original/DEFAULT/image-name")
        .map(_.get)
        .map(_.trim())
      cloneDir <- env.projectService.getCloneDirOf(project)
      runDir   <- env.projectService.getRunDirOf(project)
      projectName = "Untitled project"
      timestamp   = LocalDateTime.now().format(formatterNoSpaces)
      logFile     = "build.log"
      yaml = new UnarchiveProjectPodConfig(
        s"unarchive-${project.id}-${project.user}",
        logFile,
        env.namespace,
        session.openbisToken,
        subpath(cloneDir),
        subpath(logDir(project)),
        externalImageName(env),
        s"${env.internalRegistry}/$imageName",
        env.dss.toString(),
        datasetId
      ).toYaml()
    yield Spec(
      project,
      yaml,
      (
        projectName,
        Image(
          imageName,
          "repo2docker",
          repo2DockerVersion
        )
      ),
      sideEffect { writeFile(new File(runDir, timestamp + "_unarchive-project.yaml"), yaml) }
    )
  }
  override def createOfflineBundle(
      session: SessionInfo,
      project: Project,
      description: String
  ): Operation[Spec[LogFileName]] =
    operation { env =>
      for
        bundleDir <- env.projectService.getOfflineBundleDirOf(project)
        cloneDir  <- env.projectService.getCloneDirOf(project)
        runDir    <- env.projectService.getRunDirOf(project)
        image     <- env.projectService.getImageOf(project)
        command   <- env.serverCommandConfiguration(image, "offline")(project)
        imageName = s"${env.internalRegistry}/${image.plainName}"
        bundle <- env.bundleService.createBundle(project, description)
        yaml = new CreateOfflineBundlePodConfig(
          s"bundle-${project.id}-${project.user}",
          bundle.name + ".log",
          env.namespace,
          session.openbisToken,
          env.internalRegistry,
          imageName,
          command,
          bundle.name,
          subpath(bundleDir),
          subpath(cloneDir),
          subpath(logDir(project)),
          externalImageName(env),
          env.dss.toString()
        ).toYaml()
      yield Spec(
        project,
        yaml,
        bundle.name + ".log",
        sideEffect {
          writeFile(new File(logDir(project), bundle.name + ".log"), "Starting creation of offline bundle...\n")
          writeFile(new File(runDir, bundle.createdAt.format(formatterNoSpaces) + "_create-offline-bundle.yaml"), yaml)
        }
      )
    }

  override def exportImage(
      session: SessionInfo,
      project: Project
  ): Operation[Spec[LogFileName]] =
    operation { env =>
      for
        runDir <- env.projectService.getRunDirOf(project)
        image  <- env.projectService.getImageOf(project)
        timestamp = LocalDateTime.now().format(formatterNoSpaces)
        logFile   = timestamp + "-export.log"
        yaml = new ExportImagePodConfig(
          s"export-${project.id}-${project.user}",
          project.user,
          logFile,
          env.namespace,
          env.internalRegistry,
          image.plainName,
          env.exportRegistry,
          project.id,
          subpath(logDir(project)),
          externalImageName(env)
        ).toYaml()
      yield Spec(
        project,
        yaml,
        logFile,
        sideEffect {
          writeFile(new File(logDir(project), logFile), "Starting export of image...\n")
          writeFile(new File(runDir, timestamp + "_export.yaml"), yaml)
        }
      )
    }

  override def execute[A](spec: Spec[A]): Operation[A] =
    operation { env =>
      for
        _ <- sideEffect {
          val p = client.pods().load(new ByteArrayInputStream(spec.yaml.getBytes()))
          client.pods().create(p.get)
        }
        _ <- spec.postOperation
      yield spec.value
    }

  override def getProcessesOf(project: Project): Operation[Set[ProcessInfo]] =
    sideEffect { monitor.processesOf(project) }

  override def stop(process: ProcessId): Operation[Unit] =
    sideEffect {
      val result = client.pods().inNamespace(namespace).withName(process).delete()
      if (result == null || result == false) {
        Left(s"Stopping container $process failed, the container does not exist")
      } else {
        Right(())
      }
    }
