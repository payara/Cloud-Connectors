package fish.payara.cloud.connectors.kafka.tools;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SystemPropertiesParser {
    private final String[] propertyNames;

    public SystemPropertiesParser(String... propertyNames) {

        this.propertyNames = propertyNames;
    }

    public void applySystemProperties(Object instance, String prefix) {
        try {
            for (String name : propertyNames) {
                String value = System.getProperty(prefix + "." + name);

                if (value != null) {
                    final Field field = instance.getClass().getDeclaredField(name);
                    final Method setter = instance.getClass().getMethod(
                            "set" + name.substring(0, 1).toUpperCase() + name.substring(1),
                            field.getType());

                    setter.invoke(instance, objectToType(value, field.getType()));
                }
            }
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Failed parsing system properties: ", e);
        }

    }

    private Object objectToType(String value, Class<?> type) {
        if (Long.class.equals(type))
            return Long.getLong(value);

        if (Integer.class.equals(type))
            return Integer.getInteger(value);

        if (Boolean.class.equals(type))
            return Boolean.getBoolean(value);

        return value;
    }


}
