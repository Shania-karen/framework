package framework.helpers;


import jakarta.servlet.http.*;
import java.lang.reflect.Method;
import java.util.Map;

public class MethodInvoker {
    private final ParameterBinder binder;

    public MethodInvoker() {
        this.binder = new ParameterBinder();
    }

    public Object invoke(Method method, 
                        Object instance, 
                        HttpServletRequest req, 
                        HttpServletResponse resp,
                        Map<String, Object> requestParams,
                        Map<String, String> pathParams) throws Exception {
        
        int paramCount = method.getParameterCount();
        
        if (paramCount == 0) {
            return method.invoke(instance);
        }
        
        if (paramCount == 1) {
            return invokeWithOneParam(method, instance, req, requestParams);
        }
        
        if (paramCount == 2) {
            return method.invoke(instance, req, resp);
        }
        
        if (paramCount == 3) {
            // Vérifier le type du 3ème paramètre
            Class<?> thirdParamType = method.getParameterTypes()[2];
            if (thirdParamType == Map.class) {
                System.out.println("=== PASSAGE DES PATH PARAMS ===");
                System.out.println("pathParams: " + pathParams);
                return method.invoke(instance, req, resp, pathParams);
            }
        }
        
        throw new RuntimeException("Unsupported method signature");
    }

    private Object invokeWithOneParam(Method method, 
                                     Object instance, 
                                     HttpServletRequest req,
                                     Map<String, Object> params) throws Exception {
        Class<?> paramType = method.getParameterTypes()[0];
        
        if (paramType == HttpServletRequest.class) {
            return method.invoke(instance, req);
        } 
        
        if (paramType == Map.class) {
            return method.invoke(instance, params);
        }
        if (isSimpleType(paramType)) {
            if (!params.isEmpty()) {
                Object value = params.values().iterator().next();
                System.out.println("Valeur récupérée: " + value);
                return method.invoke(instance, value);
            }
            System.out.println(" Params vide, passage de null");
            return method.invoke(instance, (Object) null);
        }
        
        Object boundObject = binder.bind(paramType, params);
        return method.invoke(instance, boundObject);
    }
    private boolean isSimpleType(Class<?> type) {
        return type == String.class || 
               type == Integer.class || type == int.class ||
               type == Long.class || type == long.class ||
               type == Double.class || type == double.class ||
               type == Float.class || type == float.class ||
               type == Boolean.class || type == boolean.class;
    }
}
