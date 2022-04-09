package org.thermoweb.discord.database.repository;

import org.thermoweb.database.repository.Repository;
import org.thermoweb.discord.database.model.Guild;

import java.util.List;

public class GuildRepository extends Repository<Guild> {
    public List<Guild> findByCode(String code) {
        return findByQuery(String.format("select * from guilds where code = '%s'", code));
    }
}
