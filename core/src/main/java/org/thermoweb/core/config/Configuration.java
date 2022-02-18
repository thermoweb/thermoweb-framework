package org.thermoweb.core.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public class Configuration {

  private static final Properties properties = new Properties();

  public static void loadProperties(String path) throws IOException {
    properties.load(new FileInputStream(path));
  }

  public static void autoLoadProperties(String[] args) {
    String propertyPath;
    if (args.length > 0) {
      propertyPath = Path.of(args[0]).toString();
    } else {
      propertyPath =
          Optional.ofNullable(Thread.currentThread().getContextClassLoader().getResource(""))
              .orElseThrow()
              .getPath();
      propertyPath += "application.properties";
    }

    try {
      Configuration.loadProperties(propertyPath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String getProperty(Property key) {
    return properties.getProperty(key.getName());
  }
}
