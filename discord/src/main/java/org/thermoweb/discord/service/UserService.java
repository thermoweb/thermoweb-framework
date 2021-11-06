package org.thermoweb.discord.service;

import org.thermoweb.discord.database.model.User;
import org.thermoweb.discord.database.repository.UserRepository;

import java.sql.SQLException;
import java.util.List;

public class UserService {

    private final UserRepository userRepository;

    public UserService() {
        userRepository = new UserRepository();
    }


    public User getByCodeOrCreateUser(User user) {
        List<User> users = userRepository.findByCode(user.getCode());
        if (!users.isEmpty()) {
            return users.get(0);
        }

        try {
            return userRepository.save(user);
        } catch (SQLException | IllegalAccessException throwables) {
            throwables.printStackTrace();
        }

        throw new RuntimeException("failed to create or get user");
    }
}
