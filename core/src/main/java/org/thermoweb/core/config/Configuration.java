package org.thermoweb.core.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuration {

    private static final Properties properties = new Properties();

    public static void loadProperties(String path) throws IOException {
        properties.load(new FileInputStream(path));
    }

    public static String getProperty(Property key) {
        return properties.getProperty(key.getName());
    }
}
