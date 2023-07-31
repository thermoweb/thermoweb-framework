package org.thermoweb.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

public class RandomUtils {
    private static final RandomGenerator generator = RandomGenerator.getDefault();

    public static <T> T getRandomItem(List<T> list) {
        return list.get(generator.nextInt(list.size()));
    }

    public static <T> T getRandomItem(T[] array) {
        return getRandomItem(Arrays.stream(array).toList());
    }

    public static <K, V> V getRandomItem(Map<K, V> map) {
        return getRandomItem(new ArrayList<>(map.values()));
    }

    public static <T> List<T> getRandomItems(List<T> list, int numberOfItems) {
        if (list.size() <= numberOfItems) {
            return list;
        }

        List<T> elements = new ArrayList<>(List.copyOf(list));
        Set<T> randomSet = new HashSet<>();
        while(randomSet.size() < numberOfItems) {
            T element = getRandomItem(elements);
            randomSet.add(element);
            elements.remove(element);
        }
        return randomSet.stream().toList();
    }

    public static <T> List<T> getRandomItems(T[] array, int numberOfItems) {
        return getRandomItems(Arrays.stream(array).toList(), numberOfItems);
    }
}
