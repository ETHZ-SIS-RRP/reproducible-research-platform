package ch.ethz.id.sis.rrp.domain.process;

import java.net.URL;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;

public record ServerPodConfig(
                String podName,
                String logFile,
                String projectId,
                String namespace,
                String imageName,
                String command,
                String runId,
                URL openbis,
                String openbisSession,
                String sshHost,
                int sshPort,
                double memoryRequest,
                double cpuRequest,
                String repositorySubPath,
                String resultSubPath,
                String logSubPath,
                String linksSubPath,
                Function<String, String> externalImage) implements PodConfig {

        public String toYaml() throws JsonProcessingException {

                String typeName = "analysis-server";

                Volume openbisVolume = new VolumeBuilder().withName("openbis").withNewEmptyDir().endEmptyDir().build();

                EnvVar openbisToken = new EnvVarBuilder().withName("OPENBIS_TOKEN").withValue(openbisSession).build();
                EnvVar openbisUrl = new EnvVarBuilder().withName("OPENBIS_URL").withValue(openbis.getHost()).build();

                EnvVar mwiBaseUrl = new EnvVarBuilder().withName("MWI_BASE_URL").withValue("/servers/"+projectId+"/matlab").build();
                EnvVar mwiPort = new EnvVarBuilder().withName("MWI_APP_PORT").withValue("8889").build();
                EnvVar mwiAppHost = new EnvVarBuilder().withName("MWI_APP_HOST").withValue("0.0.0.0").build();
                EnvVar mwiEnableTokenAuth = new EnvVarBuilder().withName("MWI_ENABLE_TOKEN_AUTH").withValue("False").build();

                VolumeMount openbisMount = new VolumeMountBuilder()
                                .withName(openbisVolume.getName())
                                .withMountPropagation("HostToContainer")
                                .withMountPath("/mnt/" + runId)
                                .build();

                VolumeMount repoMount = new VolumeMountBuilder()
                                .withName(STORAGE_VOLUME.getName())
                                .withSubPath(repositorySubPath)
                                .withMountPath("/home/jovyan/project")
                                .build();

                VolumeMount resultMount = new VolumeMountBuilder()
                                .withName(STORAGE_VOLUME.getName())
                                .withSubPath(resultSubPath)
                                .withMountPath("/home/jovyan/results")
                                .build();
                VolumeMount linkMount = new VolumeMountBuilder()
                                .withName(STORAGE_VOLUME.getName())
                                .withSubPath(linksSubPath)
                                .withMountPath("/home/jovyan/openbis")
                                .build();
                VolumeMount logMount = new VolumeMountBuilder()
                                .withName(STORAGE_VOLUME.getName())
                                .withSubPath(logSubPath)
                                .withMountPath("/log")
                                .build();

                Container mainContainer = new ContainerBuilder()
                                .withName("main")
                                .withCommand("/bin/bash")
                                .withArgs("-c", PodConfig.trapTee(command, "/log/" + logFile))
                                .withEnv(openbisUrl, openbisToken, mwiBaseUrl, mwiPort, mwiAppHost, mwiEnableTokenAuth)
                                .withImage(imageName)
                                .withVolumeMounts(openbisMount, repoMount, resultMount, linkMount, logMount)
                                .withWorkingDir("/home/jovyan")
                                .withNewResources()
                                .withRequests(Map.of("memory", new Quantity(Double.toString(memoryRequest), "Gi"),
                                                "cpu",
                                                new Quantity(Double.toString(cpuRequest))))
                                .withLimits(Map.of("memory", new Quantity(Double.toString(memoryRequest), "Gi")))
                                .endResources()
                                .build();

                EnvVar openbisSftpHost = new EnvVarBuilder()
                                .withName("openbis_host")
                                .withValue(PodConfig.hostToIp(sshHost))
                                .build();

                EnvVar openbisSftpPort = new EnvVarBuilder().withName("openbis_port")
                                .withValue(Integer.toString(sshPort))
                                .build();

                VolumeMount openbisFuseMount = new VolumeMountBuilder()
                                .withName(openbisVolume.getName())
                                .withMountPropagation("Bidirectional")
                                .withMountPath("/openbis")
                                .build();

                Container fuseContainer = new ContainerBuilder()
                                .withName("sidecar")
                                .withEnv(openbisToken, openbisSftpHost, openbisSftpPort)
                                .withImage(externalImage.apply("fuse"))
                                .withVolumeMounts(openbisFuseMount)
                                .withNewSecurityContext()
                                .withPrivileged(true)
                                .endSecurityContext()
                                .withNewLifecycle()
                                .withNewPreStop()
                                .withNewExec()
                                .withCommand("/bin/sh", "-c", "/stop.sh")
                                .endExec()
                                .endPreStop()
                                .endLifecycle()
                                .build();

                VolumeMount openbisInitMount = new VolumeMountBuilder()
                                .withName(openbisVolume.getName())
                                .withMountPath("/openbis")
                                .build();

                Container initContainer = new ContainerBuilder()
                                .withName("init-openbis-mount")
                                .withImage("ubuntu:22.04")
                                .withCommand("sh", "-c", "mkdir -p /openbis/openbis")
                                .withVolumeMounts(openbisInitMount)
                                .build();

                Pod pod = new PodBuilder()
                                .withNewMetadata()
                                .withName(podName)
                                .withNamespace(namespace)
                                .withLabels(Map.of("name", typeName))
                                .endMetadata()
                                .withNewSpec()
                                .withHostname(podName)
                                .withSubdomain(typeName)
                                .withContainers(mainContainer, fuseContainer)
                                .withTerminationGracePeriodSeconds(5l)
                                .withInitContainers(initContainer)
                                .withRestartPolicy("Never")
                                .withVolumes(PodConfig.STORAGE_VOLUME, openbisVolume)
                                .withImagePullSecrets(PodConfig.INTERNAL_IMAGE_PULL_SECRET,
                                                PodConfig.EXTERNAL_IMAGE_PULL_SECRET)
                                .withNewSecurityContext()
                                .withRunAsUser(1000l)
                                .withRunAsGroup(1000l)
                                .withFsGroup(1000l)
                                .withFsGroupChangePolicy("OnRootMismatch")
                                .endSecurityContext()
                                .endSpec()
                                .build();

                return SerializationUtils.dumpAsYaml(pod);
        }
}
