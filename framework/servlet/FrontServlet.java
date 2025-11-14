package framework.servlet;

import framework.helpers.ComponentScan;
import framework.helpers.Mapping;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
        String path = req.getRequestURI().substring(req.getContextPath().length());
        System.out.println("Computed path = " + path);
        Mapping mapping = urlMappings.get(path);
        if (mapping != null) {
            resp.getWriter().write("Controller: " + mapping.getClassName() + ", method: " + mapping.getMethodName());
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "URL not found: " + path);
        }
    }
}
