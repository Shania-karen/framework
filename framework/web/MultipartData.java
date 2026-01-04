package framework.web;

import java.util.HashMap;
import java.util.Map;

public class MultipartData {
    private Map<String, MultipleFiles> files = new HashMap<>();
    private Map<String, Object> params = new HashMap<>();

    public Map<String, MultipleFiles> getFiles() { return files; }
    public Map<String, Object> getParams() { return params; }
}

