package framework.helpers;

public class Mapping {
    private String className;
    private String methodName;
    private String url;
    private String httpMethod; 

    public Mapping(String className, String methodName, String url) {
        this.className = className;
        this.methodName = methodName;
        this.url = url;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    @Override
    public String toString() {
        return "Mapping{" +
                "httpMethod='" + httpMethod + '\'' +
                ", url='" + url + '\'' +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}