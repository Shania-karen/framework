package framework.helpers;

import com.google.gson.Gson;

import framework.web.FileResponse;
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
    private void handleFileResponse(FileResponse fileResponse, HttpServletResponse resp) throws IOException {
        if (resp.isCommitted()) {
            System.out.println(">>> Réponse déjà envoyée, abandon");
            return;
        }
       if (fileResponse == null || fileResponse.getContent() == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Fichier vide ou inexistant");
            return;
        }
        try {
            String contentType = fileResponse.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream"; 
            }

            resp.setContentType(contentType);
            resp.setCharacterEncoding("UTF-8");

            resp.setContentLength(fileResponse.getContent().length);

            String fileName = fileResponse.getFileName();
   
            if (contentType.equals("application/pdf")) {
                resp.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
                resp.setHeader("Accept-Ranges", "bytes");
                resp.setHeader("Cache-Control", "public, max-age=3600");

                resp.setHeader("X-Content-Type-Options", "nosniff");
                resp.setHeader("X-Frame-Options", "SAMEORIGIN");
                
            } else {
        
                if (fileName != null && !fileName.isEmpty()) {
                    try {
                        String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8")
                                                .replace("+", "%20");
                        resp.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedFileName);
                    } catch (Exception e) {
                        resp.setHeader("Content-Disposition", "inline");
                    }
                } else {
                    resp.setHeader("Content-Disposition", "inline");
                }
                
                resp.setHeader("Accept-Ranges", "bytes");
                resp.setHeader("Cache-Control", "public, max-age=3600");
            }

            byte[] content = fileResponse.getContent();
            ServletOutputStream out = resp.getOutputStream();

            int bufferSize = 8192; 
            int offset = 0;
            
            while (offset < content.length) {
                int length = Math.min(bufferSize, content.length - offset);
                out.write(content, offset, length);
                offset += length;
            }
            
            out.flush();
            
            System.out.println(">>> Fichier envoyé: " + fileName + " (" + content.length + " bytes, type: " + contentType + ")");
            
        } catch (IOException e) {
            if (!e.getMessage().contains("connexion") && !e.getMessage().contains("connection")) {
                System.err.println(">>> Erreur lors de l'envoi du fichier: " + e.getMessage());
                e.printStackTrace();
            }
        }
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
        if (result instanceof FileResponse) {
            handleFileResponse((FileResponse) result, resp);
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
