package ch.ethz.id.sis.rrp.domain.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.client.internal.SerializationUtils;

public record ArchiveResultsPodConfig(
        String podName,
        String logFile,
        String namespace,
        String sessionToken,
        String resultsSubPath,
        String logSubPath,
        Function<String, String> externalImage,
        String openbis,
        String dss,
        String entityId,
        String dataSetType,
        Map<String, String> props) implements PodConfig {

    @Override
    public String toYaml() throws JsonProcessingException {

        VolumeMount resultsMount = new VolumeMountBuilder()
                .withName(PodConfig.STORAGE_VOLUME.getName())
                .withMountPath("/results")
                .withSubPath(resultsSubPath)
                .build();

        VolumeMount logMount = new VolumeMountBuilder()
                .withName(PodConfig.STORAGE_VOLUME.getName())
                .withMountPath("/log")
                .withSubPath(logSubPath)
                .build();

        List<String> properties = new ArrayList<String>();
        for (Map.Entry<String, String> prop : props.entrySet()) {
            if (!prop.getValue().trim().isEmpty()) {
                properties.addAll(Arrays.asList(
                        "--prop",
                        "'" + prop.getKey() + "'",
                        "'" + prop.getValue().replace("'", "'\"'\"'") + "'"));
            }
        }

        String command = String.format(
                "/archive-results.sh --zip %s %s %s %s %s /results %s",
                String.join(" ", properties).trim(),
                openbis,
                dss,
                sessionToken,
                entityId,
                dataSetType);

        Container mainContainer = new ContainerBuilder()
                .withName("main")
                .withImage(externalImage.apply("transport"))
                .withVolumeMounts(resultsMount, logMount)
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
                .withVolumes(PodConfig.STORAGE_VOLUME)
                .endSpec()
                .build();

        return SerializationUtils.dumpAsYaml(pod);
    }
}
