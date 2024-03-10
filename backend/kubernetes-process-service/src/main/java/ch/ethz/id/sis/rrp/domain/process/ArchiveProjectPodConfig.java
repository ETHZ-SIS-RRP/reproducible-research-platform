package ch.ethz.id.sis.rrp.domain.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

public record ArchiveProjectPodConfig(
        String podName,
        String logFile,
        String namespace,
        String sessionToken,
        String cloneSubPath,
        String logSubPath,
        Function<String, String> externalImage,
        String internalRegistry,
        String imageName,
        String toolVersion,
        String openbis,
        String dss,
        String entityId,
        String dataSetType,
        Map<String, String> props) implements PodConfig {

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

        List<String> properties = new ArrayList<String>();
        for (Map.Entry<String, String> prop : props.entrySet()) {
            if (!prop.getValue().trim().isEmpty()) {
                properties
                        .addAll(Arrays.asList(
                                "--prop",
                                "'" + prop.getKey() + "'",
                                "'" + prop.getValue().replace("'", "'\"'\"'") + "'"));
            }
        }
        String command = String.format(
                "/archive-project.sh %s %s %s %s %s %s %s %s /home/rrp/data %s",
                internalRegistry,
                imageName,
                toolVersion,
                String.join(" ", properties).trim(),
                openbis,
                dss,
                sessionToken,
                entityId,
                dataSetType);

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
