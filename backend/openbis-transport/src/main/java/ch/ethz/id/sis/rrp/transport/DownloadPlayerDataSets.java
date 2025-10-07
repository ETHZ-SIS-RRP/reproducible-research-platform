package ch.ethz.id.sis.rrp.transport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import ch.ethz.id.sis.rrp.domain.mount.MappingException;
import ch.ethz.id.sis.rrp.domain.mount.MappingReader;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;

public record DownloadPlayerDataSets(
        IDataStoreServerApi dss,
        String token,
        Path mappingFile,
        Path destinationDirectory) {

    private void downloadDataSets(String dir, List<String> permIds) {
        try {
            var path = destinationDirectory.resolve(dir);
            Files.createDirectories(path);
            for (String permId : permIds) {
                System.out.println("Downloading " + permId);
                try {
                    var downloader = new DownloadDataSet(dss, token, permId, Path.of("original"),
                            path.resolve(permId));
                    downloader.execute();
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new TransportException(e.getMessage());
        }
    }

    public void execute() throws TransportException {
        if (mappingFile.toFile().exists()) {
            try {
                var mapping = MappingReader.readMapping(mappingFile.toFile());
                mapping.destinations.forEach(this::downloadDataSets);
            } catch (MappingException e) {
                throw new TransportException(String.format("Could not read mapping from %s: %s",
                        mappingFile, e.getMessage()));
            }
        }
    }

    public static void main(String[] args) {
        try {
            var download = new DownloadPlayerDataSetsBuilder(args).build();
            download.execute();
        } catch (TransportException e) {
            System.err.println("Downloading player datasets failed");
            System.err.println(e.getMessage());
            System.exit(-1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }
}
