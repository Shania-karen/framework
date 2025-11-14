package framework.helpers;

import framework.annotation.Controller;
import framework.annotation.Get;
import framework.annotation.Post;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ComponentScan {

    public static Map<String, Mapping> scanControllers(String packageName) throws Exception {
        Map<String, Mapping> mappings = new HashMap<>();
        
        String packagePath = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        Enumeration<URL> resources = classLoader.getResources(packagePath);
        
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            
            if (resource.getProtocol().equals("file")) {
                File directory = new File(resource.getFile());
                scanDirectory(directory, packageName, mappings);
            } else if (resource.getProtocol().equals("jar")) {
                scanJar(resource, packagePath, mappings);
            }
        }
        
        return mappings;
    }

    private static void scanDirectory(File directory, String packageName, Map<String, Mapping> mappings) throws Exception {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), mappings);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                processClass(className, mappings);
            }
        }
    }

    private static void scanJar(URL resource, String packagePath, Map<String, Mapping> mappings) throws Exception {
        String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
        
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                    String className = entryName
                        .substring(0, entryName.length() - 6)
                        .replace('/', '.');
                    
                    processClass(className, mappings);
                }
            }
        }
    }

    private static void processClass(String className, Map<String, Mapping> mappings) {
        try {
            Class<?> clazz = Class.forName(className);
            
            if (clazz.isAnnotationPresent(Controller.class)) {
                Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
                String controllerPath = controllerAnnotation.value();
                
                for (Method method : clazz.getDeclaredMethods()) {
                    String fullPath = null;
                    
                    if (method.isAnnotationPresent(Get.class)) {
                        Get getAnnotation = method.getAnnotation(Get.class);
                        fullPath = controllerPath + getAnnotation.value();
                    } else if (method.isAnnotationPresent(Post.class)) {
                        Post postAnnotation = method.getAnnotation(Post.class);
                        fullPath = controllerPath + postAnnotation.value();
                    }
                    
                    if (fullPath != null) {
                        Mapping mapping = new Mapping(className, method.getName());
                        mappings.put(fullPath, mapping);
                        System.out.println("Mapped: " + fullPath + " -> " + className + "." + method.getName());
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Could not load class: " + className);
        } catch (NoClassDefFoundError e) {
       
        }
    }
}