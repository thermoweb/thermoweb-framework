package org.thermoweb.database.conf;

import org.thermoweb.core.config.Property;

public enum DatabaseConfiguration implements Property {
    URL("database.url"),
    USER("database.user"),
    PASSWORD("database.password");

    private final String name;

    DatabaseConfiguration(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
