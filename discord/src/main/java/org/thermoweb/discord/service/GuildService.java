package org.thermoweb.discord.service;

import lombok.extern.slf4j.Slf4j;
import org.thermoweb.discord.database.model.Guild;
import org.thermoweb.discord.database.repository.GuildRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class GuildService {

    private final GuildRepository guildRepository;

    public GuildService() {
        this.guildRepository = new GuildRepository();
    }

    public Optional<Guild> getOrCreateGuild(Guild guild) {
        List<Guild> guilds = guildRepository.findByCode(guild.getCode());
        if (guilds.isEmpty()) {
            try {
                return Optional.of(guildRepository.save(guild));
            } catch (SQLException | IllegalAccessException e) {
                log.error(String.format("can not save guild entity : %s\nreason : %s", guildRepository.toString(guild), e));
                return Optional.empty();
            }
        } else {
            return Optional.of(guilds.get(0));
        }
    }
}
