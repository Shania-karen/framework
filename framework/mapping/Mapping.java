package framework.mapping;

import java.lang.reflect.Method;

public class Mapping {
    private String url;
    private Class<?> controllerClass;
    private Method method;

    public Mapping(String url, Class<?> controllerClass, Method method) {
        this.url = url;
        this.controllerClass = controllerClass;
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public void setControllerClass(Class<?> controllerClass) {
        this.controllerClass = controllerClass;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "Mapping{" +
                "url='" + url + '\'' +
                ", controllerClass=" + controllerClass.getSimpleName() +
                ", method=" + method.getName() +
                '}';
    }
}