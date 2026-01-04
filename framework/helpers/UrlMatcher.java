package framework.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlMatcher {

    public static boolean matches(String pattern, String url) {
        pattern = pattern.replace("/", "\\/");
  
        pattern = pattern.replaceAll("\\{[^/]+}", "([0-9]+)");

        return url.matches(pattern);
    }
    
    public static Map<String, String> extractParams(String pattern, String url) {
        Map<String, String> params = new HashMap<>();
        List<String> paramNames = extractParamNames(pattern);

        String regex = patternToRegex(pattern);
  
        Pattern compiledPattern = Pattern.compile(regex);
        Matcher matcher = compiledPattern.matcher(url);

        if (matcher.matches()) {
            for (int i = 0; i < paramNames.size(); i++) {
                String paramName = paramNames.get(i);
                String paramValue = matcher.group(i + 1);  
                params.put(paramName, paramValue);
            }
        }
        
        return params;
    }
    
    private static String patternToRegex(String pattern) {

        String regex = pattern.replaceAll("([.+*?^$()\\[\\]|])", "\\\\$1");

        regex = regex.replaceAll("\\{[^}]+\\}", "([^/]+)");
        
        return regex;
    }
    
    private static List<String> extractParamNames(String pattern) {
        List<String> paramNames = new ArrayList<>();
        
        Pattern namePattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = namePattern.matcher(pattern);
        
        while (matcher.find()) {
            paramNames.add(matcher.group(1));
        }
        
        return paramNames;
    }
}