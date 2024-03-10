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

public record ExportImagePodConfig(
        String podName,
        String user,
        String logFile,
        String namespace,
        String internalRegistry,
        String imageName,
        String exportRegistry,
        String exportTag,
        String logSubPath,
        Function<String, String> externalImage) implements PodConfig {

    @Override
    public String toYaml() throws JsonProcessingException {

        VolumeMount logMount = new VolumeMountBuilder()
                .withName(PodConfig.STORAGE_VOLUME.getName())
                .withMountPath("/log")
                .withSubPath(logSubPath)
                .build();

        VolumeMount internalRegistryCredentialsMount = new VolumeMountBuilder()
                .withName(PodConfig.INTERNAL_CREDENTIALS_VOLUME.getName())
                .withMountPath("/etc/internal-registry-credentials")
                .withReadOnly(true)
                .build();

        VolumeMount exportRegistryCredentialsMount = new VolumeMountBuilder()
                .withName(PodConfig.EXPORT_CREDENTIALS_VOLUME.getName())
                .withMountPath("/etc/export-registry-credentials")
                .withReadOnly(true)
                .build();

        String sourceImage = internalRegistry + "/" + imageName;
        String targetImage = exportRegistry + ":" + exportTag + "-" + user;
        String command = String.format(
                "/export-image.sh %s %s %s",
                internalRegistry,
                sourceImage,
                targetImage);

        Container mainContainer = new ContainerBuilder()
                .withName("main")
                .withImage(externalImage.apply("transport"))
                .withVolumeMounts(logMount,
                        internalRegistryCredentialsMount,
                        exportRegistryCredentialsMount)
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
                .withVolumes(PodConfig.STORAGE_VOLUME, PodConfig.INTERNAL_CREDENTIALS_VOLUME,
                        PodConfig.EXPORT_CREDENTIALS_VOLUME)
                .withHostAliases(new HostAliasBuilder().withHostnames("dev-cluster.local").withIp("10.0.1.1").build())
                .endSpec()
                .build();

        return SerializationUtils.dumpAsYaml(pod);
    }
}
