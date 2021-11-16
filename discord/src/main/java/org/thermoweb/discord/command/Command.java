package org.thermoweb.discord.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface Command {
    String getTrigger();
    void execute(MessageReceivedEvent event);
}
