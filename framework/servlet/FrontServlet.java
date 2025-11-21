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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
    throws ServletException, IOException {
    
        String requestPath = request.getServletPath();
        System.out.println("\n=== REQUÊTE REÇUE ===");
        System.out.println("URL demandée : " + requestPath);
        
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
        System.out.println("Classe : " + mapping.getClassName());
        System.out.println("Méthode : " + mapping.getMethodName());
        
        // Charger la classe
        Class<?> clazz = Class.forName(mapping.getClassName());
        System.out.println("✓ Classe chargée");
        
        // Créer une instance du controller
        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
        System.out.println("✓ Instance créée : " + controllerInstance);
        
        // Récupérer la méthode
        Method method = clazz.getMethod(
            mapping.getMethodName(),
            HttpServletRequest.class,
            HttpServletResponse.class,
            Map.class
        );
        System.out.println("✓ Méthode trouvée : " + method.getName());
        
        // INVOQUER LA MÉTHODE
        System.out.println("✓ Invocation de la méthode...\n");
        method.invoke(controllerInstance, req, resp, pathParams);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
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