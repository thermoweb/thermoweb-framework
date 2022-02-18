package org.thermoweb.discord.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

public interface Command {
  String getTrigger();

  default void execute(MessageReactionAddEvent event) {
    // default method implementation
  }

  default void execute(MessageReceivedEvent event) {
    // default methode implementation
  }
}
