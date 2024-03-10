package ch.ethz.id.sis.rrp.domain.mount;

import java.util.List;
import java.util.Map;

public class DatasetMapping {
    public String serverUrl;
    public Map<String, List<String>> destinations;

    @Override
    public String toString() {
        return serverUrl + ": " + destinations;
    }
}
