package framework.helpers;

import jakarta.servlet.http.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import framework.annotation.RequestParam;
import framework.web.MultipartData;
import framework.web.MultipleFiles;
import framework.web.UploadedFile;

public class MethodInvoker {
    private final ParameterBinder binder;

    public MethodInvoker() {
        this.binder = new ParameterBinder();
    }

    public Object invoke(Method method,
                         Object instance,
                         HttpServletRequest req,
                         HttpServletResponse resp,
                         MultipartData multipartData,
                         Map<String, String> pathParams) throws Exception {

        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];

            if (p.getType().equals(HttpServletRequest.class)) {
                args[i] = req;
            } else if (p.getType().equals(HttpServletResponse.class)) {
                args[i] = resp;
            } else if (p.getType().equals(Map.class)) {
                args[i] = pathParams;
            } else if (p.getType().equals(MultipleFiles.class)) {
                // Gestion de plusieurs fichiers
                String name = p.getAnnotation(RequestParam.class).value();
                args[i] = multipartData.getFiles().get(name);
                if (args[i] == null) args[i] = new MultipleFiles();
            } else if (p.getType().equals(UploadedFile.class)) {
                // NOUVEAU : Gestion d'un seul fichier
                String name = p.getAnnotation(RequestParam.class).value();
                MultipleFiles files = multipartData.getFiles().get(name);
                if (files != null && !files.isEmpty()) {
                    args[i] = files.getFiles().get(0); // Premier fichier
                } else {
                    args[i] = null;
                }
            } else {
                // Paramètres simples
                String name = p.getAnnotation(RequestParam.class).value();
                Object value = multipartData.getParams().get(name);
                  if (value != null && p.getType() == String.class) {
                    args[i] = value.toString();
                } else {
                    args[i] = value;
                }
            }
        }

        return method.invoke(instance, args);
    }

    public Object invokeWithOneParam(Method method, 
                                     Object instance, 
                                     HttpServletRequest req,
                                     HttpServletResponse resp,
                                     MultipartData multipartData,
                                     Map<String, String> pathParams) throws Exception {

        Parameter param = method.getParameters()[0];
        Class<?> paramType = param.getType();
        Object arg = null;

        if (paramType.equals(HttpServletRequest.class)) {
            arg = req;
        } else if (paramType.equals(HttpServletResponse.class)) {
            arg = resp;
        } else if (paramType.equals(MultipleFiles.class)) {
            String name = param.getAnnotation(RequestParam.class).value();
            arg = multipartData.getFiles().get(name);
            if (arg == null) arg = new MultipleFiles();
        } else if (paramType.equals(UploadedFile.class)) {

            String name = param.getAnnotation(RequestParam.class).value();
            MultipleFiles files = multipartData.getFiles().get(name);
            if (files != null && !files.isEmpty()) {
                arg = files.getFiles().get(0);
            } else {
                arg = null;
            }
        } else if (paramType.equals(Map.class)) {
            arg = pathParams;
        } else {
            String name = param.getAnnotation(RequestParam.class).value();
              arg = multipartData.getParams().get(name);
    
            if (arg != null && paramType == String.class) {
                arg = arg.toString();
            }
        }

        return method.invoke(instance, arg);
    }

    private boolean isSimpleType(Class<?> type) {
        return type == String.class || 
               type == Integer.class || type == int.class ||
               type == Long.class || type == long.class ||
               type == Double.class || type == double.class ||
               type == Float.class || type == float.class ||
               type == Boolean.class || type == boolean.class;
    }
}