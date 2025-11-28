package framework.servlet;

import framework.helpers.ComponentScan;
import framework.helpers.Mapping;
import framework.helpers.ModelAndView;
import framework.helpers.UrlMatcher;
import framework.helpers.ParameterResolver;
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
    private ParameterResolver parameterResolver; 
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
 
        parameterResolver = new ParameterResolver();
        
        controllerPackage = config.getInitParameter("controller-package");
        System.out.println("FrontServlet initialized!");
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
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Méthode unifiée pour traiter GET et POST
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String requestPath = request.getServletPath();
        System.out.println("\n=== REQUETE REÇUE ===");
        System.out.println("Methode HTTP: " + request.getMethod());
        System.out.println("URL demandee: " + requestPath);
        
        // CHERCHER UN MAPPING CORRESPONDANT
        Mapping mapping = null;
        Map<String, String> pathParams = new HashMap<>();
        
        System.out.println("\n=== RECHERCHE DU MAPPING ===");
        for (Map.Entry<String, Mapping> entry : urlMappings.entrySet()) {
            String pattern = entry.getKey();
            System.out.println("Test du pattern: " + pattern);
            
            if (UrlMatcher.matches(pattern, requestPath)) {
                System.out.println("✓ MATCH TROUVE!");
                mapping = entry.getValue();
                pathParams = UrlMatcher.extractParams(pattern, requestPath);
                break;
            } else {
                System.out.println("✗ Pas de match");
            }
        }
        
        try {
            if (mapping != null) {
                System.out.println("\n=== PARAMETRES EXTRAITS DES PATH ===");
                for (Map.Entry<String, String> param : pathParams.entrySet()) {
                    System.out.println(param.getKey() + " = " + param.getValue());
                }
                
                // INVOQUER LA MÉTHODE AVEC LE PARAMETER RESOLVER
                invokeMethod(mapping, request, response, pathParams);
            } else {
                System.out.println("✗ AUCUN MAPPING TROUVE - 404");
                response.sendError(HttpServletResponse.SC_NOT_FOUND, 
                    "Route not found: " + requestPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Error: " + e.getMessage());
        }
    }

    /**
     * Invoque la méthode du contrôleur avec résolution automatique des paramètres
     */
    private void invokeMethod(Mapping mapping, 
                              HttpServletRequest request, 
                              HttpServletResponse response,
                              Map<String, String> pathParams) throws Exception {

        System.out.println("\n=== INVOCATION DE LA METHODE ===");
        System.out.println("Classe: " + mapping.getClassName());
        System.out.println("Methode: " + mapping.getMethodName());

        // Charger la classe du contrôleur
        Class<?> clazz = Class.forName(mapping.getClassName());
        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();

        // Trouver la méthode
        Method targetMethod = null;
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(mapping.getMethodName())) {
                targetMethod = m;
                break;
            }
        }

        if (targetMethod == null) {
            throw new RuntimeException("Method not found: " + mapping.getMethodName());
        }

        System.out.println("\n=== RESOLUTION DES PARAMETRES ===");
        
        // UTILISER LE PARAMETER RESOLVER POUR RÉSOUDRE LES PARAMÈTRES
        Object[] args = parameterResolver.resolveParameters(
            targetMethod, 
            request, 
            response, 
            pathParams
        );

        System.out.println("Nombre de parametres resolus: " + args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.println("  Param[" + i + "]: " + 
                (args[i] != null ? args[i].toString() : "null") + 
                " (" + (args[i] != null ? args[i].getClass().getSimpleName() : "null") + ")");
        }

        // INVOQUER LA MÉTHODE AVEC LES PARAMÈTRES RÉSOLUS
        Object result = targetMethod.invoke(controllerInstance, args);

        System.out.println("\n=== TRAITEMENT DU RESULTAT ===");
        System.out.println("Type de retour: " + 
            (result != null ? result.getClass().getSimpleName() : "void"));

        // GÉRER LE RÉSULTAT
        if (result instanceof String) {
            handleStringResponse((String) result, request, response);
        } else if (result instanceof ModelAndView) {
            handleModelAndView((ModelAndView) result, request, response);
        } else if (result != null) {
            handleStringResponse(result.toString(), request, response);
        }
    }

    /**
     * Gère les réponses de type String (vue ou redirection)
     */
    private void handleStringResponse(String viewName, 
                                       HttpServletRequest request,
                                       HttpServletResponse response) 
            throws ServletException, IOException {
        
        if (viewName.startsWith("redirect:")) {
            // REDIRECTION
            String redirectUrl = viewName.substring("redirect:".length());
            System.out.println("Redirection vers: " + redirectUrl);
            response.sendRedirect(request.getContextPath() + redirectUrl);
        } else {
            // FORWARD VERS JSP
            String viewPath = "/WEB-INF/views/" + viewName + ".jsp";
            System.out.println("Forward vers: " + viewPath);
            RequestDispatcher dispatcher = request.getRequestDispatcher(viewPath);
            dispatcher.forward(request, response);
        }
    }

    /**
     * Gère les réponses de type ModelAndView
     */
    private void handleModelAndView(ModelAndView mav, 
                                     HttpServletRequest request, 
                                     HttpServletResponse response) 
            throws ServletException, IOException {
        
        System.out.println("=== Handling ModelAndView ===");
        System.out.println("View name: " + mav.getViewName());
        
        // Ajouter les attributs du modèle à la requête
        for (Map.Entry<String, Object> entry : mav.getModel().entrySet()) {
            System.out.println("Attribut: " + entry.getKey() + " = " + entry.getValue());
            request.setAttribute(entry.getKey(), entry.getValue());
        }
        
        // Forward vers la vue JSP
        String viewPath = "/WEB-INF/views/" + mav.getViewName() + ".jsp";
        System.out.println("Forwarding to: " + viewPath);
        RequestDispatcher dispatcher = request.getRequestDispatcher(viewPath);
        dispatcher.forward(request, response);
    }
}