package org.thermoweb.discord.command;

import lombok.Getter;

@Getter
public abstract class InvokedCommand implements Command {
    private static String trigger;
}
