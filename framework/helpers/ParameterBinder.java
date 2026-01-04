package framework.helpers;

import java.lang.reflect.Method;
import java.util.Map;

public class ParameterBinder {

    public Object bind(Class<?> clazz, Map<String, Object> params) throws Exception {
        Object instance = clazz.getDeclaredConstructor().newInstance();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.contains(".")) {
                bindNestedProperty(instance, key, value);
            } else {
                bindSimpleProperty(instance, key, value);
            }
        }

        return instance;
    }

    private void bindSimpleProperty(Object instance, String propertyName, Object value) {
        try {
            Method setter = findSetter(instance.getClass(), propertyName);
            Object convertedValue = TypeConverter.convert(value, setter.getParameterTypes()[0]);
            setter.invoke(instance, convertedValue);
        } catch (Exception e) {
        }
    }

    private void bindNestedProperty(Object instance, String key, Object value) throws Exception {
        String[] parts = key.split("\\.", 2);
        String objectName = parts[0];
        String fieldName = parts[1];

        Method getter = instance.getClass().getMethod("get" + capitalize(objectName));
        Object subObject = getter.invoke(instance);

        if (subObject == null) {
            Class<?> subClass = getter.getReturnType();
            subObject = subClass.getDeclaredConstructor().newInstance();
            Method setter = instance.getClass().getMethod("set" + capitalize(objectName), subClass);
            setter.invoke(instance, subObject);
        }

        bindSimpleProperty(subObject, fieldName, value);
    }

    private Method findSetter(Class<?> clazz, String fieldName) throws NoSuchMethodException {
        String setterName = "set" + capitalize(fieldName);
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                return m;
            }
        }
        throw new NoSuchMethodException(setterName);
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}