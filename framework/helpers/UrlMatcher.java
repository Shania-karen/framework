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
        // Transformer {id} en groupe numérique
        pattern = pattern.replaceAll("\\{[^/]+}", "([0-9]+)");

        return url.matches(pattern);
    }
    
    /**
     * Extrait les paramètres d'une URL selon un pattern
     * 
     * @param pattern Le pattern avec des paramètres, ex: "/users/{id}/posts/{postId}"
     * @param url L'URL réelle, ex: "/users/42/posts/123"
     * @return Une Map avec les paramètres extraits: {"id": "42", "postId": "123"}
     */
    public static Map<String, String> extractParams(String pattern, String url) {
        Map<String, String> params = new HashMap<>();
        

        List<String> paramNames = extractParamNames(pattern);
        
        // 2. Convertir le pattern en regex pour capturer les VALEURS
        String regex = patternToRegex(pattern);
        
        // 3. Matcher l'URL avec la regex
        Pattern compiledPattern = Pattern.compile(regex);
        Matcher matcher = compiledPattern.matcher(url);
        
        // 4. Extraire les valeurs et les associer aux noms
        if (matcher.matches()) {
            for (int i = 0; i < paramNames.size(); i++) {
                String paramName = paramNames.get(i);
                String paramValue = matcher.group(i + 1);  // group(0) = URL complète
                params.put(paramName, paramValue);
            }
        }
        
        return params;
    }
    
    /**
     * Convertit un pattern en regex
     * "/users/{id}" → "/users/([^/]+)"
     * "/users/{id}/posts/{postId}" → "/users/([^/]+)/posts/([^/]+)"
     */
    private static String patternToRegex(String pattern) {
        // Échapper les caractères spéciaux regex (sauf les accolades)
        String regex = pattern.replaceAll("([.+*?^$()\\[\\]|])", "\\\\$1");
        
        // Remplacer {param} par un groupe de capture qui accepte tout sauf /
        regex = regex.replaceAll("\\{[^}]+\\}", "([^/]+)");
        
        return regex;
    }
    
    /**
     * Extrait les noms des paramètres d'un pattern
     * "/users/{id}/posts/{postId}" → ["id", "postId"]
     */
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