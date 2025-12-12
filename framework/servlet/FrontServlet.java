package framework.servlet;

import framework.helpers.ComponentScan;
import framework.helpers.Mapping;
import framework.helpers.ModelAndView;
import framework.helpers.UrlMatcher;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class FrontServlet extends HttpServlet {

    private Map<String, Mapping> urlMappings;
    private String controllerPackage;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        controllerPackage = config.getInitParameter("controller-package");
        System.out.println("FrontServlet initializeeeeed!");
        System.out.println("Scanning package: " + controllerPackage);  
        
        if (controllerPackage == null || controllerPackage.isEmpty()) {
            System.err.println("ERROR: controller-package parameter is null or empty!");
            return;
        }
        
        try {
            urlMappings = ComponentScan.scanControllers(controllerPackage);
               for (Map.Entry<String, Mapping> entry : urlMappings.entrySet()) {
                System.out.println(entry.getKey() + " -> " + 
                                   entry.getValue().getClassName() + "." + 
                                   entry.getValue().getMethodName());
            }
            System.out.println("Mappings loaded: " + urlMappings.keySet());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

private Object bindToObject(Class<?> clazz, Map<String, Object> model) throws Exception {
    Object instance = clazz.getDeclaredConstructor().newInstance();

    for (Map.Entry<String, Object> entry : model.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();

        if (!key.contains(".")) {
            try {
                Method setter = findSetter(clazz, key, value);
                setter.invoke(instance, convertType(value, setter.getParameterTypes()[0]));
            } catch (NoSuchMethodException e) {
                // ignorer si pas de setter
            }
        } else {
            String[] parts = key.split("\\.");
            String objectName = parts[0];
            String fieldName  = parts[1];

            Method getter = clazz.getMethod("get" + capitalize(objectName));
            Object subObj = getter.invoke(instance);

            if (subObj == null) {
                Class<?> subClass = getter.getReturnType();
                subObj = subClass.getDeclaredConstructor().newInstance();
                Method setter = clazz.getMethod("set" + capitalize(objectName), subClass);
                setter.invoke(instance, subObj);
            }

            Method subSetter = findSetter(subObj.getClass(), fieldName, value);
            subSetter.invoke(subObj, convertType(value, subSetter.getParameterTypes()[0]));
        }
    }

    return instance;
}

// Chercher le setter en ignorant la différence entre Integer et int
private Method findSetter(Class<?> clazz, String fieldName, Object value) throws NoSuchMethodException {
    String setterName = "set" + capitalize(fieldName);
    for (Method m : clazz.getMethods()) {
        if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
            return m;
        }
    }
    throw new NoSuchMethodException(setterName);
}

// Convertir Integer -> int, Double -> double etc.
private Object convertType(Object value, Class<?> targetType) {
    if (targetType.isPrimitive()) {
        if (targetType == int.class) return ((Number) value).intValue();
        if (targetType == long.class) return ((Number) value).longValue();
        if (targetType == double.class) return ((Number) value).doubleValue();
        if (targetType == float.class) return ((Number) value).floatValue();
        if (targetType == boolean.class) return value;
        if (targetType == char.class) return value;
        if (targetType == short.class) return ((Number) value).shortValue();
        if (targetType == byte.class) return ((Number) value).byteValue();
    }
    return value;
}


private String capitalize(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1);
}


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
    
        String requestPath = request.getServletPath();
        System.out.println("\n=== REQUETE REÇUE ===");
        System.out.println("URL demandee : " + requestPath);
        
        // CHERCHER UN MAPPING CORRESPONDANT
        Mapping mapping = null;
        Map<String, String> pathParams = new HashMap<>();
        
        System.out.println("\n=== RECHERCHE DU MAPPING ===");
        for (Map.Entry<String, Mapping> entry : urlMappings.entrySet()) {
            String pattern = entry.getKey();
            System.out.println("Test du pattern : " + pattern);
            
            if (UrlMatcher.matches(pattern, requestPath)) {
                System.out.println("✓ MATCH TROUVÉ !");
                mapping = entry.getValue();
                pathParams = UrlMatcher.extractParams(pattern, requestPath);
                break;
            } else {
                System.out.println("✗ Pas de match");
            }
        }
        try {
            
            if (mapping != null) {
                System.out.println("\n=== PARAMÈTRES EXTRAITS ===");
                for (Map.Entry<String, String> param : pathParams.entrySet()) {
                    System.out.println(param.getKey() + " = " + param.getValue());
                }
                
                // INVOQUER LA MÉTHODE
                invokeMethod(mapping, request, response, pathParams);
            } else {
                System.out.println("✗ AUCUN MAPPING TROUVÉ - 404");
                response.sendError(404, "Route not found");
            }
        } 
        catch (Exception e) {
            // TODO: handle exception
        }
    }

   private void invokeMethod(Mapping mapping, 
                          HttpServletRequest req, 
                          HttpServletResponse resp,
                          Map<String, String> pathParams) throws Exception {

    System.out.println("\n=== INVOCATION DE LA MÉTHODE ===");

    Class<?> clazz = Class.forName(mapping.getClassName());
    Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

    Method targetMethod = null;

    // 1) Chercher la méthode correspondant au mapping
    for (Method m : clazz.getDeclaredMethods()) {
        if (!m.getName().equals(mapping.getMethodName())) continue;

        int params = m.getParameterCount();

        if (params == 0) { // méthode vide
            targetMethod = m;
            break;
        }

        if (params == 1) {
            Class<?> paramType = m.getParameterTypes()[0];
            if (paramType == HttpServletRequest.class || paramType == Map.class) {
                targetMethod = m;
                break;
            } else {
                // Cas objet personnalisé (ex: Employe)
                targetMethod = m;
                break;
            }
        }

        if (params == 2 &&
            m.getParameterTypes()[0] == HttpServletRequest.class &&
            m.getParameterTypes()[1] == HttpServletResponse.class) {
            targetMethod = m;
            break;
        }

        if (params == 3 &&
            m.getParameterTypes()[0] == HttpServletRequest.class &&
            m.getParameterTypes()[1] == HttpServletResponse.class &&
            m.getParameterTypes()[2] == Map.class) {
            targetMethod = m;
            break;
        }
    }

    if (targetMethod == null) {
        throw new RuntimeException("No compatible method found for: " + mapping.getMethodName());
    }

    // 2) Créer un modèle de paramètres depuis la requête
    Map<String, Object> model = new HashMap<>();
    Enumeration<String> paramNames = req.getParameterNames();
    while(paramNames.hasMoreElements()){
        String key= paramNames.nextElement();
        String value = req.getParameter(key);
        Object parsedValue;
        try{
            parsedValue = Integer.parseInt(value);
        } catch(NumberFormatException e){
            try{
                parsedValue = Double.parseDouble(value);
            } catch(NumberFormatException ex){
                parsedValue = value;
            }
        }
        model.put(key, parsedValue);    
    }

    // 3) Invocation selon le type de paramètre
    Object result;

    int paramCount = targetMethod.getParameterCount();
    if (paramCount == 0) {
        result = targetMethod.invoke(controllerInstance);
    } else if (paramCount == 1) {
        Class<?> paramType = targetMethod.getParameterTypes()[0];
        if (paramType == HttpServletRequest.class) {
            result = targetMethod.invoke(controllerInstance, req);
        } else if (paramType == Map.class) {
            result = targetMethod.invoke(controllerInstance, model);
        } else {
            // Objet personnalisé
            Object obj = bindToObject(paramType, model);
            result = targetMethod.invoke(controllerInstance, obj);
        }
    } else if (paramCount == 2 &&
               targetMethod.getParameterTypes()[0] == HttpServletRequest.class &&
               targetMethod.getParameterTypes()[1] == HttpServletResponse.class) {
        result = targetMethod.invoke(controllerInstance, req, resp);
    } else if (paramCount == 3 &&
               targetMethod.getParameterTypes()[0] == HttpServletRequest.class &&
               targetMethod.getParameterTypes()[1] == HttpServletResponse.class &&
               targetMethod.getParameterTypes()[2] == Map.class) {
        result = targetMethod.invoke(controllerInstance, req, resp, model);
    } else {
        throw new RuntimeException("Unsupported method signature for: " + mapping.getMethodName());
    }

    // 4) Gestion du retour
    if (result instanceof String) {
        resp.getWriter().write((String) result);
    } else if (result instanceof ModelAndView) {
        handleModelAndView((ModelAndView) result, req, resp);
    }
}


    @Override
    
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String requestPath = req.getServletPath();
        System.out.println("\n=== POST REÇU ===");
        System.out.println("URL demandée : " + requestPath);

        Mapping mapping = null;
        Map<String, String> pathParams = new HashMap<>();

        System.out.println("\n=== RECHERCHE DU MAPPING (POST) ===");
        for (Map.Entry<String, Mapping> entry : urlMappings.entrySet()) {
            String pattern = entry.getKey();
            System.out.println("Test du pattern : " + pattern);

            if (UrlMatcher.matches(pattern, requestPath)) {
                System.out.println("✓ MATCH TROUVÉ !");
                mapping = entry.getValue();
                pathParams = UrlMatcher.extractParams(pattern, requestPath);
                break;
            }
        }

        if (mapping == null) {
            System.out.println("✗ AUCUN MAPPING TROUVÉ - 404");
            resp.sendError(404, "Route not found (POST)");
            return;
        }

        try {
            invokeMethod(mapping, req, resp, pathParams);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500, "Server error: " + e.getMessage());
        }
    }


    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        System.out.println("Computed path = " + path);
        
        Mapping mapping = urlMappings.get(path);
        
        if (mapping != null) {
            try {
                // Charger la classe du contrôleur
                Class<?> controllerClass = Class.forName(mapping.getClassName());
                
                // Créer une instance du contrôleur
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                
                // Récupérer la méthode avec ses paramètres
                Method method = null;
                for (Method m : controllerClass.getDeclaredMethods()) {
                    if (m.getName().equals(mapping.getMethodName())) {
                        method = m;
                        break;
                    }
                }
                
                if (method == null) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Method not found");
                    return;
                }
                
                // Invoquer selon le nombre de paramètres
                Object result;
                if (method.getParameterCount() == 0) {
                    result = method.invoke(controllerInstance);
                } else if (method.getParameterCount() == 2) {
                    result = method.invoke(controllerInstance, req, resp);
                } else {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                        "Unsupported method signature");
                    return;
                }
                System.out.println("Method invoked, result: " + result);
                // Gérer le résultat selon son type
                if (result instanceof ModelAndView) {
                    handleModelAndView((ModelAndView) result, req, resp);
                } else if (result instanceof String) {
                    handleStringResponse((String) result, resp);
                } else if (result != null) {
                    handleStringResponse(result.toString(), resp);
                } else {
                    handleStringResponse("Method executed successfully (no return value)", resp);
                }
                
                System.out.println("Method executed: " + mapping.getClassName() + "." + mapping.getMethodName());
                
            } catch (Exception e) {
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Error executing method: " + e.getMessage());
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "URL not found: " + path);
        }
    }

    private void handleModelAndView(ModelAndView mav, HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        System.out.println("=== Handling ModelAndView ===");
        System.out.println("View name: " + mav.getViewName());
        for (Map.Entry<String, Object> entry : mav.getModel().entrySet()) {
            req.setAttribute(entry.getKey(), entry.getValue());
        }
        
        // Forward vers la vue JSP
        String viewPath = "/WEB-INF/views/" + mav.getViewName() + ".jsp";
        System.out.println("Forwarding to: " + viewPath);
        RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
        dispatcher.forward(req, resp);
    }

    private void handleStringResponse(String content, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(content);
    }
}