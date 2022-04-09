package org.thermoweb.discord.service;

import lombok.extern.slf4j.Slf4j;
import org.thermoweb.discord.database.model.Channel;
import org.thermoweb.discord.database.repository.ChannelRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class ChannelService {

    private final ChannelRepository channelRepository;

    public ChannelService() {
        this.channelRepository = new ChannelRepository();
    }

    public Optional<Channel> getOrCreateChannel(Channel channel) {
        List<Channel> channels = channelRepository.findByCode(channel.getCode());
        if (channels.isEmpty()) {
            try {
                return Optional.of(channelRepository.save(channel));
            } catch (SQLException | IllegalAccessException e) {
                log.error(String.format("can not save channel entity : %s\nreason : %s", channelRepository.toString(channel), e));
                return Optional.empty();
            }
        } else {
            return Optional.of(channels.get(0));
        }
    }
}
