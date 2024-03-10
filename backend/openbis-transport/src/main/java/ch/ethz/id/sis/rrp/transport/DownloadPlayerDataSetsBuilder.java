package ch.ethz.id.sis.rrp.transport;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

class DownloadPlayerDataSetsBuilder extends Builder<DownloadPlayerDataSets> {

    private static Function<Parser, Void> argumentMapping = parser -> {
        parser.addArgument("dss")
                .dest("dss")
                .help("DSS URL");

        parser.addArgument("token")
                .dest("token")
                .help("openBIS session token");

        parser.addArgument("dataset-mapping-file")
                .dest("file")
                .help("Path to dataset mapping file");

        parser.addArgument("directory")
                .dest("directory")
                .help("Directory to which download the dataset");
        return null;
    };

    public DownloadPlayerDataSetsBuilder(String[] args) {
        super(args, "Reproducible Research Platform / DownloadPlayerDataSets", argumentMapping);
    }

    @Override
    public DownloadPlayerDataSets createFrom(Namespace ns) throws ArgumentParserException {
        IDataStoreServerApi dss = HttpInvokerUtils.createStreamSupportingServiceStub(
                IDataStoreServerApi.class,
                ns.get("dss") + IDataStoreServerApi.SERVICE_URL,
                Integer.MAX_VALUE);
        String token = ns.get("token");
        Path mappingFile = Paths.get((String) ns.get("file"));
        Path destinationDir = Paths.get((String) ns.get("directory"));

        checkThat(mappingFile, this::isInputFile);
        checkThat(destinationDir, this::isOutputDirectory);

        return new DownloadPlayerDataSets(dss, token, mappingFile, destinationDir);
    }
}
