package org.thermoweb.discord.listener;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.thermoweb.discord.command.Command;

import javax.annotation.Nonnull;
import java.util.List;

public class CommandListener extends ListenerAdapter {

  private final List<Command> commandList;

  public CommandListener(List<Command> commandList) {
    this.commandList = commandList;
  }

  @Override
  public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
    commandList.stream()
        .filter(c -> event.getMessage().getContentRaw().contains(c.getTrigger()))
        .findFirst()
        .ifPresent(c -> c.execute(event));
  }
}
