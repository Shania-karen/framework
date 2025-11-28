package framework.helpers;

import framework.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;


public class ParameterResolver {
    

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

            if (paramType == HttpServletRequest.class) {
                resolvedParams[i] = request;
                System.out.println("  [" + i + "] HttpServletRequest injecte");
                continue;
            }
            

            if (paramType == HttpServletResponse.class) {
                resolvedParams[i] = response;
                System.out.println("  [" + i + "] HttpServletResponse injecte");
                continue;
            }
            

            if (paramType == Map.class) {
                resolvedParams[i] = pathParams;
                System.out.println("  [" + i + "] Map<String,String> (pathParams) injecte");
                continue;
            }

            String paramName = null;
            boolean required = true;
            String defaultValue = "";

            if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam annotation = param.getAnnotation(RequestParam.class);
                paramName = annotation.value();
                required = annotation.required();
                defaultValue = annotation.defaultValue();
                System.out.println("  [" + i + "] @RequestParam detecte: " + paramName);
            } else {
               
                paramName = param.getName();
                System.out.println("  [" + i + "] Parametre sans annotation: " + paramName);
                
             
                if (paramName.startsWith("arg")) {
                    System.out.println("      ATTENTION: Nom générique détecté (" + paramName + ")");
                    System.out.println("      Conseil: Compilez avec -parameters ou utilisez @RequestParam");
                    
             
                    Map<String, String[]> allParams = request.getParameterMap();
                    if (!allParams.isEmpty()) {
                        paramName = allParams.keySet().iterator().next();
                        System.out.println("      Tentative avec le premier paramètre trouvé: " + paramName);
                        required = false; 
                    }
                }
            }
            
  
            String paramValue = request.getParameter(paramName);
            

            if (paramValue == null && pathParams != null) {
                paramValue = pathParams.get(paramName);
            }
            

            if (paramValue == null || paramValue.isEmpty()) {
                if (required && defaultValue.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Parametre requis manquant: " + paramName + 
                        "\nSolution 1: Compilez avec l'option -parameters" +
                        "\nSolution 2: Utilisez @RequestParam(\"" + paramName + "\")"
                    );
                }
                paramValue = defaultValue.isEmpty() ? null : defaultValue;
            }
            
            System.out.println("  Valeur trouvee: " + paramValue);
            
      
            resolvedParams[i] = convertParameter(paramValue, paramType, paramName);
        }
        
        return resolvedParams;
    }

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
           
                return value.equalsIgnoreCase("true") 
                    || value.equals("1") 
                    || value.equalsIgnoreCase("on")
                    || value.equalsIgnoreCase("yes");
            } 
            else if (targetType == String[].class) {
        
                return value.split(",");
            }
            else {
                
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