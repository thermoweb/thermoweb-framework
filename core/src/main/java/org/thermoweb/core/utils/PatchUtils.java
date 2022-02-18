package org.thermoweb.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PatchUtils {
    private static final ObjectMapper mapper;
    private static final Set<Class<?>> BASE_TYPES =
            new HashSet<>(
                    Arrays.asList(
                            String.class,
                            Boolean.class,
                            Character.class,
                            Byte.class,
                            Short.class,
                            Integer.class,
                            Long.class,
                            Float.class,
                            Double.class,
                            BigDecimal.class,
                            Void.class));

    static {
        mapper = new ObjectMapper();
        //FIXME: have to handle later time module
//        mapper.registerModule(new JavaTimeModule());
//        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private PatchUtils() {}

    public static List<String> diff(Object o1, Object o2) throws IllegalAccessException {
        return difference(o1, o2, null);
    }

    private static List<String> difference(Object o1, Object o2, String parent)
            throws IllegalAccessException {
        ArrayList<String> diffs = new ArrayList<>();
        if (parent == null) {
            parent = o1.getClass().getSimpleName();
        }

        for (Field field : o1.getClass().getDeclaredFields()) {
            field.setAccessible(true); // FIXME: find better solution than this
            Object value1 = field.get(o1);
            Object value2 = field.get(o2);

            if (value1 == null && value2 == null) {
                continue;
            }

            if (value1 == null || value2 == null) {
                diffs.add(parent + "." + field.getName());
            } else {
                if (isBaseType(value1.getClass()) || value1.getClass().isEnum()) {
                    if (!Objects.equals(value1, value2)) {
                        diffs.add(parent + "." + field.getName());
                    }
                } else {
                    diffs.addAll(difference(value1, value2, parent + "." + field.getName()));
                }
            }
        }

        return diffs;
    }

    private static boolean isBaseType(Class<?> clazz) {
        return BASE_TYPES.contains(clazz);
    }

}
