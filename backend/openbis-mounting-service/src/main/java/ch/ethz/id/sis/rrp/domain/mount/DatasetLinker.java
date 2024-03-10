package ch.ethz.id.sis.rrp.domain.mount;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.DataSet;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.fetchoptions.DataSetFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.DataSetPermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.dataset.id.IDataSetId;

public record DatasetLinker(IApplicationServerApi openbis, String token, String openbisPath) {

    public void createLinks(File dir, DatasetMapping mapping) {
        List<String> dataSetIds = mapping.destinations.values().stream().flatMap(List::stream)
                .collect(Collectors.toList());
        var info = readInfo(dataSetIds);

        mapping.destinations.entrySet().forEach(entry -> {
            var path = entry.getKey();
            var root = new File(dir, path);
            root.mkdirs();
            entry.getValue().forEach(dataSetId -> {
                var dataset = info.get(dataSetId);
                var target = dataset == null ? "unknown" : getPathOf(dataset);
                try {
                    Files.createSymbolicLink(new File(root, dataSetId).toPath(), Paths.get(target));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private String getPathOf(DataSet dataset) {
        return String.join("/",
                List.of(openbisPath,
                        dataset.getExperiment().getIdentifier().getIdentifier().substring(1),
                        dataset.getPermId().getPermId()));
    }

    private Map<String, DataSet> readInfo(List<String> ids) {
        List<? extends IDataSetId> idList = ids.stream().map(DataSetPermId::new).collect(Collectors.toList());
        var fetch = new DataSetFetchOptions();
        fetch.withExperiment();
        return openbis.getDataSets(token, idList, fetch).values().stream().collect(Collectors.toMap(
                entry -> entry.getPermId().toString(),
                entry -> entry));
    }
}
