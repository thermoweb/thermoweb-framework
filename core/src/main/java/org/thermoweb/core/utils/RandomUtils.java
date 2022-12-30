package org.thermoweb.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

public class RandomUtils {
    private static final RandomGenerator generator = RandomGenerator.getDefault();

    public static <T> T getRandomItem(List<T> list) {
        return list.get(generator.nextInt(list.size()));
    }

    public static <K, V> V getRandomItem(Map<K, V> map) {
        return getRandomItem(new ArrayList<>(map.values()));
    }
}
