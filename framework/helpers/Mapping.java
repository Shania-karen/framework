package framework.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mapping {
    private String className;
    private String methodName;
    private List<String> pathParams; 


    public Mapping(String className, String methodName, String path) {
        this.className = className;
        this.methodName = methodName;
        this.pathParams = extractPathParams(path);
    }


    private static List<String> extractPathParams(String path) {
        List<String> params = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(path);
        while (matcher.find()) {
            params.add(matcher.group(1));
        }
        return params;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }
    public List<String> getPathParams() {
        return pathParams;
    }
}

