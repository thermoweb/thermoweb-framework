package org.thermoweb.discord.database.repository;

import org.thermoweb.database.repository.Repository;
import org.thermoweb.discord.database.model.Channel;

import java.util.List;

public class ChannelRepository extends Repository<Channel> {

    public List<Channel> findByCode(String code) {
        return findByQuery(String.format("select * from channels where code = '%s'", code));
    }
}
