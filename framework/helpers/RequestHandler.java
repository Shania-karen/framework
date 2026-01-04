package framework.helpers;

import jakarta.servlet.http.*;
import java.lang.reflect.Method;
import java.util.*;

import framework.web.MultipartData;
import framework.web.MultipleFiles;
import framework.web.UploadedFile;

public class RequestHandler {

    private final MethodInvoker methodInvoker;

    public RequestHandler() {
        this.methodInvoker = new MethodInvoker();
    }

    public Object handleRequest(Mapping mapping,
                                HttpServletRequest req,
                                HttpServletResponse resp,
                                Map<String, String> pathParams) throws Exception {

        Class<?> controllerClass = Class.forName(mapping.getClassName());
        Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

        Method method = findMethod(controllerClass, mapping.getMethodName());
        if (method == null) throw new RuntimeException("Method not found: " + mapping.getMethodName());

        MultipartData multipartData = extractMultipartData(req);

        return methodInvoker.invoke(method, controllerInstance, req, resp, multipartData, pathParams);
    }

    private Method findMethod(Class<?> clazz, String methodName) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) return m;
        }
        return null;
    }

    private MultipartData extractMultipartData(HttpServletRequest req) throws Exception {
        MultipartData data = new MultipartData();

        if (req.getContentType() != null && req.getContentType().startsWith("multipart/form-data")) {

            for (Part part : req.getParts()) {
                String fieldName = part.getName();
                String fileName = part.getSubmittedFileName();

                if (fileName != null) {
                    byte[] content = part.getInputStream().readAllBytes();
                    UploadedFile uploadedFile = new UploadedFile(fileName, part.getContentType(), content);

                    MultipleFiles mf = data.getFiles().get(fieldName);
                    if (mf == null) {
                        mf = new MultipleFiles();
                        data.getFiles().put(fieldName, mf);
                    }
                    mf.addFile(uploadedFile);

                } else {
                    String value = new String(part.getInputStream().readAllBytes());
                    data.getParams().put(fieldName, parseValue(value));
                }
            }

        } else {
            Enumeration<String> names = req.getParameterNames();
            while (names.hasMoreElements()) {
                String key = names.nextElement();
                data.getParams().put(key, parseValue(req.getParameter(key)));
            }
        }

        return data;
    }

    private Object parseValue(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e1) {
            try { return Double.parseDouble(value); }
            catch (NumberFormatException e2) { return value; }
        }
    }
}
