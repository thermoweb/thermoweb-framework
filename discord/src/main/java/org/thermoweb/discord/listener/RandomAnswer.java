package org.thermoweb.discord.listener;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.thermoweb.core.config.Configuration;
import org.thermoweb.discord.database.model.Answer;
import org.thermoweb.discord.service.AnswerService;
import org.thermoweb.discord.service.UserService;

import java.util.List;

import static org.thermoweb.discord.conf.DiscordBotConf.SELF_ID;

public class RandomAnswer extends ListenerAdapter {

    private final UserService userService;
    private final AnswerService answerService;

    public RandomAnswer() {
        this.userService = new UserService();
        this.answerService = new AnswerService();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        List<User> mentionedUsers = event.getMessage().getMentionedUsers();
        if (!mentionedUsers.isEmpty()
                && mentionedUsers.stream().anyMatch(u -> u.getId().equals(Configuration.getProperty(SELF_ID)))) {
            net.dv8tion.jda.api.entities.User author = event.getMessage().getAuthor();
            org.thermoweb.discord.database.model.User user = org.thermoweb.discord.database.model.User.builder()
                    .code(author.getId())
                    .name(author.getName())
                    .build();
            user = userService.getByCodeOrCreateUser(user);
            Answer answer = answerService.getRandomAnswerForUser(user).orElseThrow();
            String message = answerService.createAnswerForUser(answer, user);
            MessageAction messageAction = event.getMessage().reply(String.format(message, author.getId()));
            if (answer.getEmbeddedImg() != null) {
                messageAction = messageAction.embed(new EmbedBuilder().setImage(answer.getEmbeddedImg()).build());
            }

            messageAction.queue();
            answerService.addAnswerHistory(answer, user);
        }
    }
}
