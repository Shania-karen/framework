package framework.helpers;

public class TypeConverter {

    public static Object convert(Object value, Class<?> targetType) {
        if (!targetType.isPrimitive()) {
            return value;
        }

        if (!(value instanceof Number)) {
            return value;
        }

        Number num = (Number) value;

        if (targetType == int.class) return num.intValue();
        if (targetType == long.class) return num.longValue();
        if (targetType == double.class) return num.doubleValue();
        if (targetType == float.class) return num.floatValue();
        if (targetType == short.class) return num.shortValue();
        if (targetType == byte.class) return num.byteValue();

        return value;
    }
}