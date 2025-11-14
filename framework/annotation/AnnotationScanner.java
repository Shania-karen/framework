package framework.annotation;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.lang.annotation.Annotation;

public class AnnotationScanner {

    public static List<Class<?>> getClassesWithAnnotation(String packageName, Class<? extends Annotation> annotationClass) {
        List<Class<?>> classesWithAnnotation = new ArrayList<>();
        try {
            String path = packageName.replace('.', '/');
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(path);

            if (resource == null) {
                System.out.println("Package non trouvé : " + packageName);
                return classesWithAnnotation;
            }

            File directory = new File(resource.getFile());
            if (!directory.exists()) {
                return classesWithAnnotation;
            }

            for (String fileName : directory.list()) {
                if (fileName.endsWith(".class")) {
                    String className = packageName + "." + fileName.substring(0, fileName.length() - 6);
                    Class<?> clazz = Class.forName(className);

                    if (clazz.isAnnotationPresent(annotationClass)) {
                        classesWithAnnotation.add(clazz);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return classesWithAnnotation;
    }
}

