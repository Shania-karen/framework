package framework.annotation;

import framework.enum.NestedLoadStrategy;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface NestedObject {
    NestedLoadStrategy strategy() default NestedLoadStrategy.BY_ID;
    String idParam();
}

