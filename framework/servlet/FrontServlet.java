package framework.servlet;

import framework.helpers.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FrontServlet extends HttpServlet {
    private Map<String, Mapping> urlMappings;
    private RequestHandler requestHandler;
    private ResponseHandler responseHandler;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String controllerPackage = config.getInitParameter("controller-package");
        
        System.out.println("FrontServlet initialized!");
        System.out.println("Scanning package: " + controllerPackage);
        
        if (controllerPackage == null || controllerPackage.isEmpty()) {
            throw new ServletException("controller-package parameter is required");
        }
        
        try {
            urlMappings = ComponentScan.scanControllers(controllerPackage);
            requestHandler = new RequestHandler();
            responseHandler = new ResponseHandler();
            
            System.out.println("Mappings loaded: " + urlMappings.size());
        } catch (Exception e) {
            throw new ServletException("Failed to initialize servlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String requestPath = req.getServletPath();
        String httpMethod = req.getMethod(); 
        
        System.out.println("\n=== REQUEST: " + httpMethod + " " + requestPath);
        
        try {

            MappingResult mappingResult = findMapping(requestPath, httpMethod);
            
            if (mappingResult == null) {
                resp.sendError(404, "Route not found: " + httpMethod + " " + requestPath);
                return;
            }

            Object result = requestHandler.handleRequest(
                mappingResult.getMapping(), 
                req, 
                resp, 
                mappingResult.getPathParams()
            );

            responseHandler.handleResponse(result, mappingResult.getMapping(), req, resp);
            
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500, "Server error: " + e.getMessage());
        }
    }

    private MappingResult findMapping(String requestPath, String httpMethod) {
        System.out.println("Recherche mapping pour: " + httpMethod + " " + requestPath);
        
        String exactKey = httpMethod + ":" + requestPath;
        if (urlMappings.containsKey(exactKey)) {
            Mapping mapping = urlMappings.get(exactKey);
            Map<String, String> pathParams = new HashMap<>();
            return new MappingResult(mapping, pathParams);
        }

        for (Map.Entry<String, Mapping> entry : urlMappings.entrySet()) {
            String key = entry.getKey();
  
            String[] parts = key.split(":", 2);
            if (parts.length != 2) continue;
            
            String mappingMethod = parts[0]; 
            String pattern = parts[1];        
            
            if (mappingMethod.equalsIgnoreCase(httpMethod) && UrlMatcher.matches(pattern, requestPath)) {
                System.out.println(" Match trouvé: " + key);
                Map<String, String> pathParams = UrlMatcher.extractParams(pattern, requestPath);
                return new MappingResult(entry.getValue(), pathParams);
            }
        }
        
        System.out.println(" Aucun mapping trouvé");
        return null;
    }
}
