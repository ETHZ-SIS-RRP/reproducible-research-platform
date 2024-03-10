package ch.ethz.id.sis.rrp.transport;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import ch.ethz.sis.openbis.generic.dssapi.v3.IDataStoreServerApi;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

class DownloadDataSetBuilder extends Builder<DownloadDataSet> {

    private static Function<Parser, Void> argumentMapping = parser -> {
        parser.addArgument("dss")
                .dest("dss")
                .help("DSS URL");

        parser.addArgument("token")
                .dest("token")
                .help("openBIS session token");

        parser.addArgument("perm-id")
                .dest("permId")
                .help("Dataset ID");

        parser.addArgument("directory")
                .dest("directory")
                .help("Directory to which download the dataset");
        return null;
    };

    public DownloadDataSetBuilder(String[] args) {
        super(args, "Reproducible Research Platform / DownloadDataSet", argumentMapping);
    }

    @Override
    public DownloadDataSet createFrom(Namespace ns) throws ArgumentParserException {

        IDataStoreServerApi dss = HttpInvokerUtils.createStreamSupportingServiceStub(
                IDataStoreServerApi.class,
                ns.get("dss") + IDataStoreServerApi.SERVICE_URL,
                Integer.MAX_VALUE);
        String token = ns.get("token");
        String permId = ns.get("permId");
        Path directory = Paths.get((String) ns.get("directory"));

        checkThat(directory, this::isOutputDirectory);
        return new DownloadDataSet(dss, token, permId, Path.of("original"), directory);
    }
}
