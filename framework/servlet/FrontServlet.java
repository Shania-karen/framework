package framework.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import framework.helpers.ComponentScan;
import framework.helpers.Mapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.io.*;


public class FrontServlet extends HttpServlet {

    private Map<String, Mapping> urlMappings = new HashMap<>();
    private String controllerPackage;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.controllerPackage = config.getInitParameter("controller-package");
        if (this.controllerPackage == null || this.controllerPackage.isEmpty()) {
            throw new ServletException("The 'controller-package' init-param is missing or empty in web.xml");
        }
        try {
            this.urlMappings = ComponentScan.scanControllers(this.controllerPackage);
        } catch (Exception e) {
            throw new ServletException("Failed to scan controllers", e);
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

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        out.println("URI = " + req.getRequestURI());
        out.println("ServletPath = " + req.getServletPath());
        out.println("PathInfo = " + req.getPathInfo());
        String path = req.getPathInfo();
        if (path == null || path.isEmpty()) {
            path = req.getServletPath();
            out.println("pathhhhh" + path);
        }

        Mapping mapping = urlMappings.get(path);

        if (mapping == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "URL not found: " + path);
            return;
        }

        try {
            Class<?> clazz = Class.forName(mapping.getClassName());
            Object controllerInstance = clazz.getConstructor().newInstance();
            Method method = clazz.getMethod(mapping.getMethodName());

            Object result = method.invoke(controllerInstance);

            out.println("Mapped method found for url: " + path);
            if (result instanceof String) {
                out.println("Method value:");
                out.println(result);
            }

        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error executing method");
            e.printStackTrace(out);
        }
    }

}