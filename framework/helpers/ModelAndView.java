package framework.helpers;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {
    private String viewName;
    private Map<String, Object> model;

    public ModelAndView() {
        this.model = new HashMap<>();
    }

    public ModelAndView(String viewName) {
        this.viewName = viewName;
        this.model = new HashMap<>();
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public Map<String, Object> getModel() {
        return model;
    }

    public void addObject(String key, Object value) {
        this.model.put(key, value);
    }

    public void addAllObjects(Map<String, Object> objects) {
        this.model.putAll(objects);
    }
}