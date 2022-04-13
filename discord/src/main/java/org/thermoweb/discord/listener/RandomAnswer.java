package org.thermoweb.discord.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.thermoweb.core.config.Configuration;
import org.thermoweb.discord.database.model.Answer;
import org.thermoweb.discord.database.model.Channel;
import org.thermoweb.discord.service.AnswerService;
import org.thermoweb.discord.service.ChannelService;
import org.thermoweb.discord.service.GuildService;
import org.thermoweb.discord.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.thermoweb.discord.conf.DiscordBotConf.SELF_ID;

@Slf4j
public class RandomAnswer extends ListenerAdapter {

    private final UserService userService;
    private final AnswerService answerService;
    private final GuildService guildService;
    private final ChannelService channelService;

    private int pastResponsesUndiplicateLimit;

    public RandomAnswer() {
        this.userService = new UserService();
        this.answerService = new AnswerService();
        this.guildService = new GuildService();
        this.channelService = new ChannelService();
        this.pastResponsesUndiplicateLimit = 10;
    }

    public RandomAnswer(int pastResponsesUndiplicateLimit) {
        this();
        this.pastResponsesUndiplicateLimit = pastResponsesUndiplicateLimit;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        List<User> mentionedUsers = event.getMessage().getMentionedUsers();
        if (!mentionedUsers.isEmpty()
                && mentionedUsers.stream().anyMatch(u -> u.getId().equals(Configuration.getProperty(SELF_ID)))) {
            var author = event.getMessage().getAuthor();
            var user = org.thermoweb.discord.database.model.User.builder()
                    .code(author.getId())
                    .name(author.getName())
                    .build();
            user = userService.getByCodeOrCreateUser(user);
            Answer answer = answerService.getRandomAnswerForUser(user, pastResponsesUndiplicateLimit).orElseThrow();
            String message = answerService.createAnswerForUser(answer, user);

            log.debug(String.format("randow answer for user %s(%d) : %s", user.getName(), user.getId(), message));

            MessageAction messageAction = event.getMessage().reply(String.format(message, author.getId()));
            if (answer.getEmbeddedImg() != null) {
                messageAction = messageAction.setEmbeds(new EmbedBuilder().setImage(answer.getEmbeddedImg()).build());
            }

            messageAction.queue();
            MessageChannel channelMessage = event.getChannel();
            Guild guildMessage = event.getGuild();

            var guild = guildService.getOrCreateGuild(org.thermoweb.discord.database.model.Guild.builder()
                            .code(guildMessage.getId())
                            .name(guildMessage.getName())
                            .owner(Optional.ofNullable(guildMessage.getOwner())
                                    .map(Member::getUser)
                                    .map(this::getOrCreateUser)
                                    .orElse(null))
                            .build())
                    .orElse(null);


            Channel channel = channelService.getOrCreateChannel(Channel.builder()
                            .guild(guild)
                            .code(channelMessage.getId())
                            .name(channelMessage.getName())
                            .build())
                    .orElse(null);

            answerService.addAnswerHistory(answer, user, channel, event.getMessage().getContentDisplay());
        }
    }

    private org.thermoweb.discord.database.model.User getOrCreateUser(User userData) {
        return userService.getByCodeOrCreateUser(org.thermoweb.discord.database.model.User.builder()
                .code(userData.getId())
                .name(userData.getName())
                .build());
    }
}
