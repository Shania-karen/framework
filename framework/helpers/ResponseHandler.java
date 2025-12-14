package framework.helpers;

import com.google.gson.Gson;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

public class ResponseHandler {
    private final Gson gson;

    public ResponseHandler() {
        this.gson = new Gson();
    }

    public void handleResponse(Object result, 
                              Mapping mapping,
                              HttpServletRequest req, 
                              HttpServletResponse resp) 
            throws Exception {
        
        if (result instanceof ModelAndView) {
            handleModelAndView((ModelAndView) result, req, resp);
            return;
        }

        if (shouldReturnJson(mapping)) {
            handleJsonResponse(result, resp);
            return;
        }

        handleTextResponse(result, resp);
    }

    private boolean shouldReturnJson(Mapping mapping) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(mapping.getClassName());
        Method method = findMethod(clazz, mapping.getMethodName());
        
        boolean isRestController = clazz.isAnnotationPresent(
            framework.annotation.RestController.class);
        boolean hasResponseBody = method != null && 
            method.isAnnotationPresent(framework.annotation.ResponseBody.class);
        
        return isRestController || hasResponseBody;
    }

    private Method findMethod(Class<?> clazz, String methodName) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                return m;
            }
        }
        return null;
    }

    private void handleModelAndView(ModelAndView mav, 
                                   HttpServletRequest req, 
                                   HttpServletResponse resp) 
            throws ServletException, IOException {
        
        for (Map.Entry<String, Object> entry : mav.getModel().entrySet()) {
            req.setAttribute(entry.getKey(), entry.getValue());
        }
        
        String viewPath = "/WEB-INF/views/" + mav.getViewName() + ".jsp";
        RequestDispatcher dispatcher = req.getRequestDispatcher(viewPath);
        dispatcher.forward(req, resp);
    }

    private void handleJsonResponse(Object result, HttpServletResponse resp) 
            throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(result));
    }

    private void handleTextResponse(Object result, HttpServletResponse resp) 
            throws IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(result != null ? result.toString() : "");
    }
}
