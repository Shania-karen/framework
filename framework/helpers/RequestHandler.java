package framework.helpers;

import jakarta.servlet.http.*;
import java.lang.reflect.Method;
import java.util.*;

public class RequestHandler {
    private final ParameterBinder parameterBinder;
    private final MethodInvoker methodInvoker;

    public RequestHandler() {
        this.parameterBinder = new ParameterBinder();
        this.methodInvoker = new MethodInvoker();
    }

    public Object handleRequest(Mapping mapping, 
                               HttpServletRequest req, 
                               HttpServletResponse resp,
                               Map<String, String> pathParams) throws Exception {
        
        Class<?> controllerClass = Class.forName(mapping.getClassName());
        Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
        
        Method method = findMethod(controllerClass, mapping.getMethodName());
        if (method == null) {
            throw new RuntimeException("Method not found: " + mapping.getMethodName());
        }
        
        Map<String, Object> requestParams = extractRequestParameters(req);
        
        return methodInvoker.invoke(method, controllerInstance, req, resp, requestParams, pathParams);
    }

    private Method findMethod(Class<?> clazz, String methodName) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                return m;
            }
        }
        return null;
    }

    private Map<String, Object> extractRequestParameters(HttpServletRequest req) {
        Map<String, Object> model = new HashMap<>();
        Enumeration<String> paramNames = req.getParameterNames();
        
        while (paramNames.hasMoreElements()) {
            String key = paramNames.nextElement();
            String value = req.getParameter(key);
            model.put(key, parseValue(value));
        }
        
        return model;
    }

    private Object parseValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                return value;
            }
        }
    }
}
