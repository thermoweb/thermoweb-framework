package org.thermoweb.discord.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public interface Command {
    String name();

    String description();

    default void execute(MessageReactionAddEvent event) {
        // default method implementation
    }

    default void execute(SlashCommandInteractionEvent event) {
        // default method implementation
    }


    default CommandData getCommandData() {
        return Commands.slash(name(), description());
    }
}
