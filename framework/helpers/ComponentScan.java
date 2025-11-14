package framework.helpers;

import framework.annotation.Controller;
import framework.annotation.Get;
import framework.annotation.Post;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.JarURLConnection;
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
            System.out.println("Found resource: " + resource);

            String protocol = resource.getProtocol();

            if ("file".equals(protocol)) {
                // Scanne les classes dans WEB-INF/classes
                File directory = new File(resource.toURI());
                if (directory.exists()) {
                    scanDirectory(directory, packageName, mappings, classLoader);
                }
            } else if ("jar".equals(protocol)) {
                // Scanne les classes du JAR, mais **ignore les controllers** du projet
                if (!packageName.startsWith("framework.controller")) {
                    scanJar(resource, packageName, mappings, classLoader);
                } else {
                    System.out.println("Skipping scanning controllers from JAR");
                }
            } else {
                System.out.println("Skipping resource with protocol: " + protocol);
            }
        }

        return mappings;
    }

    private static void scanDirectory(File directory, String packageName, Map<String, Mapping> mappings, ClassLoader cl) throws Exception {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), mappings, cl);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replaceAll("\\.class$", "");
                processClass(className, mappings, cl);
            }
        }
    }

    private static void scanJar(URL resource, String packageName, Map<String, Mapping> mappings, ClassLoader cl) throws Exception {
        JarURLConnection jarConn = (JarURLConnection) resource.openConnection();
        try (JarFile jar = jarConn.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            String packagePath = packageName.replace('.', '/');

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.endsWith(".class") && entryName.startsWith(packagePath)) {
                    String className = entryName.replace('/', '.').replaceAll("\\.class$", "");
                    processClass(className, mappings, cl);
                }
            }
        }
    }

    private static void processClass(String className, Map<String, Mapping> mappings, ClassLoader cl) {
        try {
            Class<?> clazz = cl.loadClass(className);

            if (clazz.isAnnotationPresent(Controller.class)) {
                System.out.println("Found controller class: " + className);
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
            System.err.println("Cannot load class (dependency missing?): " + className);
        }
    }
}
