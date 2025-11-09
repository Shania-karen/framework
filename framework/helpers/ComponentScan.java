package framework.helpers;


import framework.annotation.Controller;
import framework.helpers.Mapping;
import framework.mapping.UrlMapping;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ComponentScan {

    public static Map<String, Mapping> scanControllers(String packageName) throws Exception {
        Map<String, Mapping> urlMappings = new HashMap<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL resource = classLoader.getResource(path);

        if (resource == null) {
            throw new IllegalArgumentException("Package not found: " + packageName);
        }

        File directory = new File(resource.toURI());
        for (File file : directory.listFiles()) {
            if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(Controller.class)) {
                    Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
                    String baseUrl = controllerAnnotation.value();

                    for (Method method : clazz.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(UrlMapping.class)) {
                            UrlMapping urlAnnotation = method.getAnnotation(UrlMapping.class);
                            String methodUrl = urlAnnotation.value();
                            String fullUrl = (baseUrl + methodUrl).replaceAll("/+", "/");

                            if (urlMappings.containsKey(fullUrl)) {
                                throw new IllegalStateException("Duplicate URL found: " + fullUrl);
                            }

                            urlMappings.put(fullUrl, new Mapping(clazz.getName(), method.getName()));
                        }
                    }
                }
            }
        }
        return urlMappings;
    }
}
