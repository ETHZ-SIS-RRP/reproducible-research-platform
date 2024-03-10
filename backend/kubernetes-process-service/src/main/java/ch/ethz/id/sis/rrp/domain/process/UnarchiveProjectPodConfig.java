package ch.ethz.id.sis.rrp.domain.process;

import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HostAliasBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;

public record UnarchiveProjectPodConfig(
        String podName,
        String logFile,
        String namespace,
        String sessionToken,
        String cloneSubPath,
        String logSubPath,
        Function<String, String> externalImage,
        String imageName,
        String dss,
        String datasetId) implements PodConfig {

    @Override
    public String toYaml() throws JsonProcessingException {

        VolumeMount repositoryMount = new VolumeMountBuilder()
                .withName(PodConfig.STORAGE_VOLUME.getName())
                .withMountPath("/repo")
                .withSubPath(cloneSubPath)
                .build();

        VolumeMount internalRegistryCredentialsMount = new VolumeMountBuilder()
                .withName(PodConfig.INTERNAL_CREDENTIALS_VOLUME.getName())
                .withMountPath("/etc/registry-credentials")
                .withReadOnly(true)
                .build();

        VolumeMount logMount = new VolumeMountBuilder()
                .withName(PodConfig.STORAGE_VOLUME.getName())
                .withMountPath("/log")
                .withSubPath(logSubPath)
                .build();

        String command = String.format(
                "/unarchive-project.sh %s %s %s %s",
                imageName,
                dss,
                sessionToken,
                datasetId);

        Container mainContainer = new ContainerBuilder()
                .withName("main")
                .withImage(externalImage.apply("transport"))
                .withVolumeMounts(repositoryMount, internalRegistryCredentialsMount, logMount)
                .withCommand("/bin/bash")
                .withArgs("-c", PodConfig.trapTee(command, "/log/" + logFile))
                .build();

        Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName(podName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withHostname(podName)
                .withContainers(mainContainer)
                .withTerminationGracePeriodSeconds(30l)
                .withImagePullSecrets(PodConfig.EXTERNAL_IMAGE_PULL_SECRET)
                .withRestartPolicy("Never")
                .withVolumes(PodConfig.STORAGE_VOLUME, PodConfig.INTERNAL_CREDENTIALS_VOLUME)
                .withHostAliases(new HostAliasBuilder().withHostnames("dev-cluster.local").withIp("10.0.1.1").build())
                .endSpec()
                .build();

        return SerializationUtils.dumpAsYaml(pod);
    }
}
