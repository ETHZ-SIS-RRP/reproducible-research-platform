package ch.ethz.id.sis.rrp.domain.process;

import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HostAliasBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;

public record BuildFromGitPodConfig(
                String podName,
                String internalRegistry,
                String imageName,
                String namespace,
                String cloneSubPath,
                String logSubPath,
                Function<String, String> externalImage) implements PodConfig {

        @Override
        public String toYaml() throws JsonProcessingException {

                Volume dockerRunVolume = new VolumeBuilder().withName("docker-run").withNewEmptyDir().endEmptyDir()
                                .build();
                Volume dockerLocalVolume = new VolumeBuilder().withName("docker-local").withNewEmptyDir()
                                .endEmptyDir().build();

                VolumeMount dockerRunMountMain = new VolumeMountBuilder()
                                .withName(dockerRunVolume.getName())
                                .withMountPath("/docker-run")
                                .build();

                VolumeMount dockerRunMountSidecar = new VolumeMountBuilder()
                                .withName(dockerRunVolume.getName())
                                .withMountPath("/home/rrp/.docker/run")
                                .build();

                VolumeMount dockerLocalMount = new VolumeMountBuilder()
                                .withName(dockerLocalVolume.getName())
                                .withMountPath("/home/rrp/.local")
                                .build();

                VolumeMount repositoryMount = new VolumeMountBuilder()
                                .withName(PodConfig.STORAGE_VOLUME.getName())
                                .withMountPath("/repository")
                                .withSubPath(cloneSubPath)
                                .build();

                VolumeMount logMount = new VolumeMountBuilder()
                                .withName(PodConfig.STORAGE_VOLUME.getName())
                                .withMountPath("/log")
                                .withSubPath(logSubPath)
                                .build();

                VolumeMount internalRegistryCredentialsMount = new VolumeMountBuilder()
                                .withName(PodConfig.INTERNAL_CREDENTIALS_VOLUME.getName())
                                .withMountPath("/etc/registry-credentials")
                                .withReadOnly(true)
                                .build();

                EnvVar imageNameEnvVar = new EnvVarBuilder().withName("image_name").withValue(imageName).build();

                Container repo2DockerContainer = new ContainerBuilder()
                                .withName("main")
                                .withImage(externalImage.apply("repo2docker"))
                                .withVolumeMounts(
                                                dockerRunMountMain,
                                                repositoryMount,
                                                internalRegistryCredentialsMount,
                                                logMount)
                                .withEnv(imageNameEnvVar)
                                .build();

                EnvVar registryEnvVar = new EnvVarBuilder().withName("registry").withValue(internalRegistry).build();

                Container dockerDaemonContainer = new ContainerBuilder()
                                .withName("sidecar")
                                .withImage(externalImage.apply("dockerd"))
                                .withVolumeMounts(
                                                dockerRunMountSidecar,
                                                dockerLocalMount,
                                                repositoryMount,
                                                internalRegistryCredentialsMount)
                                .withNewSecurityContext()
                                .withPrivileged(true)
                                .endSecurityContext()
                                .withEnv(registryEnvVar)
                                .build();

                Pod pod = new PodBuilder()
                                .withNewMetadata()
                                .withName(podName)
                                .withNamespace(namespace)
                                .endMetadata()
                                .withNewSpec()
                                .withHostname(podName)
                                .withContainers(repo2DockerContainer, dockerDaemonContainer)
                                .withTerminationGracePeriodSeconds(30l)
                                .withImagePullSecrets(PodConfig.EXTERNAL_IMAGE_PULL_SECRET)
                                .withRestartPolicy("Never")
                                .withVolumes(PodConfig.STORAGE_VOLUME, PodConfig.INTERNAL_CREDENTIALS_VOLUME,
                                                dockerRunVolume,
                                                dockerLocalVolume)
                                .withHostAliases(new HostAliasBuilder().withHostnames("dev-cluster.local")
                                                .withIp("10.0.1.1").build())
                                .endSpec()
                                .build();

                return SerializationUtils.dumpAsYaml(pod);
        }
}
