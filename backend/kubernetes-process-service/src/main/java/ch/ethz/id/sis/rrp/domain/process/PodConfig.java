package ch.ethz.id.sis.rrp.domain.process;

import java.net.URL;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;

public interface PodConfig {

    public static final Volume STORAGE_VOLUME = new VolumeBuilder()
            .withName("storage")
            .withNewPersistentVolumeClaim()
            .withClaimName("platform")
            .endPersistentVolumeClaim()
            .build();

    public static final LocalObjectReference INTERNAL_IMAGE_PULL_SECRET = new LocalObjectReferenceBuilder()
            .withName("internal-registry-credentials").build();

    public static final LocalObjectReference EXTERNAL_IMAGE_PULL_SECRET = new LocalObjectReferenceBuilder()
            .withName("external-registry-credentials").build();

    public static final Volume INTERNAL_CREDENTIALS_VOLUME = new VolumeBuilder()
            .withName("registry-credentials")
            .withSecret(
                    new SecretVolumeSourceBuilder()
                            .withSecretName("internal-registry-credentials").build())
            .build();

    public static final Volume EXPORT_CREDENTIALS_VOLUME = new VolumeBuilder()
            .withName("export-registry-credentials")
            .withSecret(
                    new SecretVolumeSourceBuilder()
                            .withSecretName("export-registry-credentials").build())
            .build();

    public static String urlToIp(String input) {
        try {
            URL url = new URL(input);
            return new URL(url.getProtocol(),
                    java.net.InetAddress.getByName(url.getHost()).getHostAddress(),
                    url.getPort(),
                    url.getFile()).toString();
        } catch (Exception e) {
            return input;
        }
    }

    public static String hostToIp(String host) {
        try {
            return java.net.InetAddress.getByName(host).getHostAddress();
        } catch (Exception e) {
            return host;
        }

    }

    public static String trapTee(String command, String logFile) {
        String trap = """
                trap "rv=\\${PIPESTATUS[0]}; if [ \\"\\$rv\\" -eq \\"0\\" ]; then touch LOGFILE.rrp; else echo \\"\\$rv\\" > LOGFILE.rrp; fi" EXIT && """;
        trap = trap.replaceAll("LOGFILE", logFile);

        String postfix = String.format(" 2>&1 | tee -a %s", logFile);
        return trap + command + postfix;
    }

    public String toYaml() throws JsonProcessingException;
}
