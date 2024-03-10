package ch.ethz.id.sis.rrp.transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpMethod;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.TableModel;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.fetchoptions.DataStoreFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.id.DataStorePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.datastore.search.DataStoreSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.Experiment;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.fetchoptions.ExperimentFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.search.ExperimentSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.execute.AggregationServiceExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.id.DssServicePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.service.id.IDssServiceId;
import ch.systemsx.cisd.common.http.JettyHttpClientFactory;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;

public record CreateDataSet(
        String openbisUrl,
        String dssUrl,
        String token,
        String entityId,
        Path directory,
        String dataSetType,
        Map<String, String> properties,
        boolean zip) {

    private List<File> contentOf(File item) {
        if (item.getName().startsWith(".")) {
            return Collections.emptyList();
        } else if (item.isFile()) {
            return Collections.singletonList(item);
        } else {
            return Arrays.asList(item.listFiles()).stream()
                    .flatMap(file -> contentOf(file).stream())
                    .collect(Collectors.toList());
        }
    }

    private Optional<ExperimentIdentifier> getExperimentIdentifier(IApplicationServerApi openbis,
            String entityId) {
        ExperimentSearchCriteria experimentSearchCriteria = new ExperimentSearchCriteria();
        experimentSearchCriteria.withIdentifier().thatEquals(entityId);
        experimentSearchCriteria.withOrOperator().withPermId().thatEquals(entityId);
        SearchResult<Experiment> experiments = openbis.searchExperiments(token, experimentSearchCriteria,
                new ExperimentFetchOptions());
        return experiments.getObjects().stream().map(e -> e.getIdentifier()).findFirst();
    }

    private Optional<SampleIdentifier> getSampleIdentifier(IApplicationServerApi openbis,
            String entityId) {
        SampleSearchCriteria sampleSearchCriteria = new SampleSearchCriteria();
        sampleSearchCriteria.withIdentifier().thatEquals(entityId);
        sampleSearchCriteria.withOrOperator().withPermId().thatEquals(entityId);
        SearchResult<Sample> samples = openbis.searchSamples(token,
                sampleSearchCriteria,
                new SampleFetchOptions());
        return samples.getObjects().stream().map(e -> e.getIdentifier()).findFirst();
    }

    private Iterable<byte[]> streamFile(File file, int chunkSize) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(file);

        return new Iterable<>() {

            public Iterator<byte[]> iterator() {
                return new Iterator<byte[]>() {
                    public boolean hasMore = true;

                    public boolean hasNext() {
                        return hasMore;
                    }

                    public byte[] next() {
                        try {
                            byte[] bytes = inputStream.readNBytes(chunkSize);
                            if (bytes.length < chunkSize) {
                                hasMore = false;
                                inputStream.close();
                            }
                            return bytes;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        };

    }

    private void execute() throws TransportException {

        File sourceDirectory = directory.toFile();

        List<File> content = contentOf(sourceDirectory);
        if (content.isEmpty()) {
            throw new TransportException("The directory " + directory + " is empty");
        }

        if (zip) {
            try {
                content = Collections.singletonList(ZipTools.zipDirectory(sourceDirectory));
            } catch (IOException e) {
                e.printStackTrace();
                throw new TransportException("Zipping dataset files failed");
            }
        }

        long totalSize = 0;
        for (File file : content) {
            try {
                totalSize += Files.size(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                throw new TransportException(
                        "Could not read the size of file " + file.getAbsolutePath());
            }
        }

        long timeout = totalSize * 5 * 60000 / (1024 * 1024 * 1024);
        timeout = Long.max(timeout, 15000l);

        IApplicationServerApi openbis = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class,
                openbisUrl + IApplicationServerApi.SERVICE_URL, timeout);

        System.out.println("Total files " + content.size() + ", total size " + totalSize
                + " bytes, setting timeout to "
                + (timeout / 1000) + " seconds");

        Optional<ExperimentIdentifier> experimentId = getExperimentIdentifier(openbis, entityId);
        Optional<SampleIdentifier> sampleId = getSampleIdentifier(openbis, entityId);

        if (!experimentId.isPresent() && !sampleId.isPresent()) {
            throw new TransportException("Could not find an entity with identifier " + entityId);
        }

        String dataStoreId = openbis
                .searchDataStores(token, new DataStoreSearchCriteria(), new DataStoreFetchOptions())
                .getObjects().get(0).getCode();

        System.out.println("Using datastore " + dataStoreId);

        HttpClient client = JettyHttpClientFactory.getHttpClient();
        int chunkSize = 1024 * 1024;
        List<String> fileNames = new ArrayList<>();
        List<Integer> acceptableStatuses = Arrays.asList(200, 502);
        int id = 1;
        try {
            for (File file : content) {
                String fileName = file.getAbsolutePath()
                        .substring(sourceDirectory.getAbsolutePath().length() + 1);
                if (zip) {
                    fileName = "dataset-content.zip";
                }
                fileNames.add(fileName);

                String prefix = "(" + id + "/" + content.size() + ") " + fileName + ": ";

                long size = Files.size(file.toPath());
                long totalChunks = (size / chunkSize) + (size % chunkSize == 0 ? 0 : 1);
                System.out.println(prefix + "Starting upload of " + size + " bytes");

                long start = 0;
                for (var chunk : streamFile(file, chunkSize)) {
                    if (chunk.length == 0) {
                        continue;
                    }
                    long end = start + chunk.length;

                    int status = 0;
                    while (status != 200) {
                        ContentProvider contentProvider = new BytesContentProvider(chunk);

                        Request httpRequest = client.newRequest(dssUrl + "/session_workspace_file_upload")
                                .method(HttpMethod.POST);
                        httpRequest.param("sessionID", token);
                        httpRequest.param("id", Integer.toString(id++));
                        httpRequest.param("filename", fileName);
                        httpRequest.param("startByte", Long.toString(start));
                        httpRequest.param("endByte", Long.toString(end));
                        httpRequest.content(contentProvider);
                        ContentResponse response = httpRequest.send();

                        status = response.getStatus();
                        System.out.println(
                                prefix + "Chunk " + (start / chunkSize + 1) + "/" + totalChunks
                                        + " uploaded with status " + status);

                        if (!acceptableStatuses.contains(status)) {
                            throw new IOException(response.getContentAsString());
                        }
                    }
                    start += chunkSize;
                }
                System.out.println(prefix + "Upload complete");
            }
        } catch (IOException | TimeoutException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new TransportException(e.getClass().getName());
        }

        IDssServiceId serviceId = new DssServicePermId("dataset-uploader-api", new DataStorePermId(dataStoreId));

        AggregationServiceExecutionOptions options = new AggregationServiceExecutionOptions();
        options.withParameter("method", "insertDataSet")
                .withParameter("dataSetType", dataSetType)
                .withParameter("folderName", "")
                .withParameter("fileNames", fileNames)
                .withParameter("isZipDirectoryUpload", zip)
                .withParameter("properties", properties)
                .withParameter("parentIdentifiers", new ArrayList<String>());

        if (experimentId.isPresent()) {
            options.withParameter("experimentIdentifier", experimentId.get().getIdentifier());
        } else if (sampleId.isPresent()) {
            options.withParameter("sampleIdentifier", sampleId.get().getIdentifier());
        } else {
            throw new TransportException("Could not find an entity with identifier " + entityId);
        }

        System.out.println("Creating dataset");
        TableModel response = openbis.executeAggregationService(token, serviceId, options);
        System.out.println(response.getRows());
    }

    public static void main(String[] args) {
        try {
            CreateDataSet createDataSet = new CreateDataSetBuilder(args).build();
            createDataSet.execute();
        } catch (TransportException e) {
            System.err.println("Creating dataset failed");
            System.err.println(e.getMessage());
            System.exit(-1);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }
}
