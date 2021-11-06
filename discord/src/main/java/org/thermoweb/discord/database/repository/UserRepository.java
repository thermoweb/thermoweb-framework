package org.thermoweb.discord.database.repository;

import lombok.NoArgsConstructor;
import org.thermoweb.database.repository.Repository;
import org.thermoweb.discord.database.model.User;

import java.util.List;

@NoArgsConstructor
public class UserRepository extends Repository<User> {

    public List<User> findByCode(String code) {
        return findByQuery(String.format("select * from users where code = '%s'", code));
    }
}
