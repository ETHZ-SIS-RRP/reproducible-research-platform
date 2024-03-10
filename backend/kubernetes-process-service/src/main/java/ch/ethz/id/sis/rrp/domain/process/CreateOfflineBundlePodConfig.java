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

public record CreateOfflineBundlePodConfig(
        String podName,
        String logFile,
        String namespace,
        String sessionToken,
        String internalRegistry,
        String imageName,
        String cmd,
        String bundleName,
        String bundleSubPath,
        String cloneSubPath,
        String logSubPath,
        Function<String, String> externalImage,
        String dss) implements PodConfig {

    @Override
    public String toYaml() throws JsonProcessingException {

        VolumeMount bundleMount = new VolumeMountBuilder()
                .withName(PodConfig.STORAGE_VOLUME.getName())
                .withMountPath("/repo")
                .withSubPath(cloneSubPath)
                .withReadOnly(true)
                .build();

        VolumeMount cloneMount = new VolumeMountBuilder()
                .withName(PodConfig.STORAGE_VOLUME.getName())
                .withMountPath("/output")
                .withSubPath(bundleSubPath)
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

        String command = String.format(
                "/create-offline-bundle.sh %s %s %s %s %s %s",
                imageName,
                dss,
                sessionToken,
                bundleName,
                internalRegistry,
                cmd);

        Container mainContainer = new ContainerBuilder()
                .withName("main")
                .withImage(externalImage.apply("transport"))
                .withVolumeMounts(bundleMount, cloneMount, logMount,
                        internalRegistryCredentialsMount)
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
