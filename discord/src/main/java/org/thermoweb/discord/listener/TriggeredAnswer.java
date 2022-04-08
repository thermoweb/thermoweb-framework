package org.thermoweb.discord.listener;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

@RequiredArgsConstructor
@Builder
@Slf4j
public class TriggeredAnswer extends ListenerAdapter {

    private final List<String> triggeredWords;
    private final String answer;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (isTriggered(event.getMessage().getContentRaw())) {
            log.debug(String.format("triggered anwser with : %s", event.getMessage().getContentRaw()));
            log.debug("repling to that user : " + answer);
            event.getMessage().reply(answer).queue();
        }
    }

    private boolean isTriggered(String phrase) {
        for (String unsafeWords : triggeredWords) {
            if (phrase.toLowerCase().contains(unsafeWords)) {
                log.debug("triggered by " + unsafeWords);
                return true;
            }
        }

        return false;
    }
}
