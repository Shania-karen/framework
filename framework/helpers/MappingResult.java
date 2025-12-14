package framework.helpers;

import java.util.Map;

public class MappingResult {
    private final Mapping mapping;
    private final Map<String, String> pathParams;

    public MappingResult(Mapping mapping, Map<String, String> pathParams) {
        this.mapping = mapping;
        this.pathParams = pathParams;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }
}