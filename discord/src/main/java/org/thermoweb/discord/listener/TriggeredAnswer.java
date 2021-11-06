package org.thermoweb.discord.listener;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

@RequiredArgsConstructor
@Builder
public class TriggeredAnswer extends ListenerAdapter {

    private final List<String> triggeredWords;
    private final String answer;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (isTriggered(event.getMessage().getContentRaw())) {
            event.getMessage().reply(answer).queue();
        }
    }

    private boolean isTriggered(String phrase) {
        for (String unsafeWords : triggeredWords) {
            if (phrase.toLowerCase().contains(unsafeWords)) {
                return true;
            }
        }

        return false;
    }
}
