package framework.servlet;

import framework.helpers.ComponentScan;
import framework.helpers.Mapping;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
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
            System.out.println("Mappings loaded: " + urlMappings.keySet());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
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
                
                // Afficher le résultat
                resp.setContentType("text/html");
                resp.setCharacterEncoding("UTF-8");
                
                if (result != null) {
                    resp.getWriter().write(result.toString());
                } else {
                    resp.getWriter().write("Method executed successfully (no return value)");
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
}