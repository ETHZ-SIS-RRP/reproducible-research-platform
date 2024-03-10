package ch.ethz.id.sis.rrp.transport;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

class CreateDataSetBuilder extends Builder<CreateDataSet> {

    private static Function<Parser, Void> argumentMapping = parser -> {
        parser.addArgument("openbis")
                .dest("openbis")
                .help("openBIS URL");

        parser.addArgument("dss")
                .dest("dss")
                .help("DSS URL");

        parser.addArgument("token")
                .dest("token")
                .help("openBIS session token");

        parser.addArgument("entity-id")
                .dest("entityId")
                .help("Entity identifier");

        parser.addArgument("directory")
                .dest("directory")
                .help("Directory that contains the files for the dataset");

        parser.addArgument("dataset-type")
                .dest("dataSetType")
                .help("Dataset type");

        parser.addArgument("--prop")
                .dest("prop")
                .nargs(2)
                .action(Arguments.append())
                .help("Dataset properties");

        parser.addArgument("--zip")
                .dest("zip")
                .action(Arguments.storeTrue())
                .help("Upload to openBIS as a zip file");
        return null;
    };

    public CreateDataSetBuilder(String[] args) {
        super(args, "Reproducible Research Platform / CreateDataSet", argumentMapping);
    }

    @Override
    public CreateDataSet createFrom(Namespace ns) throws ArgumentParserException {
        String openbis = ns.get("openbis");
        String dss = ns.get("dss");
        String token = ns.get("token");
        String entityId = ns.get("entityId");
        Path directory = Paths.get((String) ns.get("directory"));
        String dataSetType = ns.get("dataSetType");
        List<List<String>> properties = ns.getList("prop");
        boolean zip = ns.getBoolean("zip");

        var props = new HashMap<String, String>();
        if (properties != null) {
            for (List<String> prop : properties) {
                props.put(prop.get(0), prop.get(1));
            }
        }

        checkThat(directory, this::isInputDirectory);

        return new CreateDataSet(openbis, dss, token, entityId, directory, dataSetType, props, zip);
    }
}
