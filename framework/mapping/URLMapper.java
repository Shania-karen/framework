package framework.mapping;

import framework.annotation.Controller;
import framework.annotation.Get;
import framework.annotation.AnnotationScanner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class URLMapper {
    
    private Map<String, Mapping> urlMappings;

    public URLMapper() {
        this.urlMappings = new HashMap<>();
    }

    public void scanPackage(String packageName) {
     
        List<Class<?>> controllers = AnnotationScanner.getClassesWithAnnotation(packageName, Controller.class);
  
        for (Class<?> controller : controllers) {
            Method[] methods = controller.getDeclaredMethods();
            
            for (Method method : methods) {
                if (method.isAnnotationPresent(Get.class)) {
                    Get getAnnotation = method.getAnnotation(Get.class);
                    String url = getAnnotation.value();
                    

                    Mapping mapping = new Mapping(url, controller, method);
                    urlMappings.put(url, mapping);
                }
            }
        }
    }


    public Mapping getMapping(String url) {
        return urlMappings.get(url);
    }

    public Map<String, Mapping> getAllMappings() {
        return urlMappings;
    }

    public void printAllMappings() {
        System.out.println("=== Mappings trouvés ===");
        for (Map.Entry<String, Mapping> entry : urlMappings.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }
}