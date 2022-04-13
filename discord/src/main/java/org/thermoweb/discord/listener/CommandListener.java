package org.thermoweb.discord.listener;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.thermoweb.discord.command.Command;

import java.util.List;

public class CommandListener extends ListenerAdapter {

    private final List<Command> commandList;

    public CommandListener(List<Command> commandList) {
        this.commandList = commandList;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        commandList.stream()
                .filter(c -> event.getName().equals(c.name()))
                .findFirst()
                .ifPresent(c -> c.execute(event));
    }
}
