package org.thermoweb.discord.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.thermoweb.discord.command.Command;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class ReactionListener extends ListenerAdapter {

  private final Map<String, Command> commandList;

  public ReactionListener(Map<String, Command> commandList) {
    this.commandList = commandList;
  }

  @Override
  public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
    super.onMessageReactionAdd(event);
    String emote = event.getReactionEmote().getName();
    Optional.ofNullable(commandList.get(emote)).ifPresent(cmd -> cmd.execute(event));
    log.debug(event.toString());
  }
}
