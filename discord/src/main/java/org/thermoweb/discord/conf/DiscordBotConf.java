package org.thermoweb.discord.conf;

import org.thermoweb.core.config.Property;

public enum DiscordBotConf implements Property {
    TOKEN("token");

    private final String name;

    DiscordBotConf(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
