package ch.ethz.id.sis.rrp.domain.mount;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;

import org.apache.commons.text.CaseUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

public class MappingReader {

    public static DatasetMapping readMapping(String spec) throws MappingException {
        Constructor c = new Constructor(DatasetMapping.class);

        c.setPropertyUtils(new PropertyUtils() {
            @Override
            public Property getProperty(Class<? extends Object> type, String name) {
                name = CaseUtils.toCamelCase(name, false, '-', '_');
                return super.getProperty(type, name);
            }
        });

        Yaml yaml = new Yaml(c);
        try {
            return yaml.load(new StringReader(spec));
        } catch (YAMLException e) {
            throw new MappingException("Could not parse YAML", e);
        }
    }

    public static DatasetMapping readMapping(File spec) throws MappingException {
        try {
            return readMapping(Files.readString(spec.toPath()));
        } catch (IOException e) {
            throw new MappingException("Could not read file " + spec.getAbsolutePath(), e);
        }
    }
}
