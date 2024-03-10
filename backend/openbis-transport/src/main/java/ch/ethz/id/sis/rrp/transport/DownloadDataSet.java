package ch.ethz.id.sis.rrp.transport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownload;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadOptions;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.download.DataSetFileDownloadReader;
import ch.ethz.sis.openbis.generic.dssapi.v3.dto.datasetfile.id.DataSetFilePermId;

public record DownloadDataSet(
        IDataStoreServerApi dss,
        String token,
        String permId,
        Path sourcePath,
        Path targetDirectory) {

    public void execute() throws TransportException {
        var datasetId = new DataSetPermId(permId);
        var root = new DataSetFilePermId(datasetId, sourcePath.toString());
        var options = new DataSetFileDownloadOptions();
        options.setRecursive(true);

        var downloadInputStream = dss.downloadFiles(token, Arrays.asList(root), options);
        var reader = new DataSetFileDownloadReader(downloadInputStream);
        DataSetFileDownload file = null;

        try {
            while ((file = reader.read()) != null) {
                var dsFile = file.getDataSetFile();
                if (!dsFile.isDirectory()) {
                    String path = dsFile.getPath();
                    long size = file.getDataSetFile().getFileLength();
                    System.out.printf("Downloading %s (%s bytes)\n", path, size);
                    File targetFile =
                            new File(targetDirectory.toFile(), file.getDataSetFile().getPath());
                    targetFile.getParentFile().mkdirs();
                    FileUtils.copyInputStreamToFile(file.getInputStream(), targetFile);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            var download = new DownloadDataSetBuilder(args).build();
            download.execute();
        } catch (TransportException e) {
            System.err.println("Downloading dataset failed");
            System.err.println(e.getMessage());
            System.exit(-1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }
}
