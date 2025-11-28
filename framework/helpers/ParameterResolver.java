package framework.helpers;

import framework.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * Classe responsable de la résolution des paramètres des méthodes de contrôleur
 */
public class ParameterResolver {
    
    /**
     * Résout tous les paramètres nécessaires pour invoquer une méthode
     * 
     * @param method La méthode à invoquer
     * @param request La requête HTTP
     * @param response La réponse HTTP
     * @param pathParams Les paramètres extraits de l'URL (ex: /user/{id})
     * @return Un tableau d'objets contenant les valeurs des paramètres
     */
    public Object[] resolveParameters(Method method, 
                                      HttpServletRequest request,
                                      HttpServletResponse response,
                                      Map<String, String> pathParams) throws Exception {
        
        Parameter[] parameters = method.getParameters();
        Object[] resolvedParams = new Object[parameters.length];
        
        System.out.println("Resolution de " + parameters.length + " parametres:");
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();
            
            // CAS 1: HttpServletRequest
            if (paramType == HttpServletRequest.class) {
                resolvedParams[i] = request;
                System.out.println("  [" + i + "] HttpServletRequest injecte");
                continue;
            }
            
            // CAS 2: HttpServletResponse
            if (paramType == HttpServletResponse.class) {
                resolvedParams[i] = response;
                System.out.println("  [" + i + "] HttpServletResponse injecte");
                continue;
            }
            
            // CAS 3: Map (pathParams)
            if (paramType == Map.class) {
                resolvedParams[i] = pathParams;
                System.out.println("  [" + i + "] Map<String,String> (pathParams) injecte");
                continue;
            }
            
            // CAS 4: Paramètres de formulaire/query avec @RequestParam ou par nom
            String paramName = null;
            boolean required = true;
            String defaultValue = "";
            
            // Vérifier si @RequestParam est présent
            if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam annotation = param.getAnnotation(RequestParam.class);
                paramName = annotation.value();
                required = annotation.required();
                defaultValue = annotation.defaultValue();
                System.out.println("  [" + i + "] @RequestParam detecte: " + paramName);
            } else {
                // Pas d'annotation: utiliser le nom de la variable Java
                paramName = param.getName();
                System.out.println("  [" + i + "] Parametre sans annotation: " + paramName);
            }
            
            // Chercher la valeur dans les paramètres de requête
            String paramValue = request.getParameter(paramName);
            
            // Si pas trouvé, chercher dans pathParams
            if (paramValue == null && pathParams != null) {
                paramValue = pathParams.get(paramName);
            }
            
            // Gérer les valeurs manquantes
            if (paramValue == null || paramValue.isEmpty()) {
                if (required && defaultValue.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Parametre requis manquant: " + paramName
                    );
                }
                paramValue = defaultValue.isEmpty() ? null : defaultValue;
            }
            
            System.out.println("      Valeur trouvee: " + paramValue);
            
            // Convertir en type approprié
            resolvedParams[i] = convertParameter(paramValue, paramType, paramName);
        }
        
        return resolvedParams;
    }
    
    /**
     * Convertit une valeur String en type Java approprié
     */
    private Object convertParameter(String value, Class<?> targetType, String paramName) {
        if (value == null) {
            return getDefaultValue(targetType);
        }
        
        try {
            if (targetType == String.class) {
                return value;
            } 
            else if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value);
            } 
            else if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value);
            } 
            else if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value);
            } 
            else if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value);
            } 
            else if (targetType == boolean.class || targetType == Boolean.class) {
                // Accepter "true", "1", "on", "yes"
                return value.equalsIgnoreCase("true") 
                    || value.equals("1") 
                    || value.equalsIgnoreCase("on")
                    || value.equalsIgnoreCase("yes");
            } 
            else if (targetType == String[].class) {
                // Pour les checkboxes multiples ou select multiple
                return value.split(",");
            }
            else {
                // Type non supporté, retourner en String
                System.out.println("      ATTENTION: Type " + targetType.getSimpleName() + 
                                 " non supporté, retourne String");
                return value;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Impossible de convertir '" + value + "' en " + 
                targetType.getSimpleName() + " pour le parametre: " + paramName
            );
        }
    }
    
    /**
     * Retourne la valeur par défaut pour un type primitif
     */
    private Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == char.class) return '\u0000';
        return null;
    }
}