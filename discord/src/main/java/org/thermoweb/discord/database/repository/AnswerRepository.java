package org.thermoweb.discord.database.repository;

import lombok.NoArgsConstructor;
import org.thermoweb.database.repository.Repository;
import org.thermoweb.discord.database.model.Answer;
import org.thermoweb.discord.database.model.User;

import java.util.List;

@NoArgsConstructor
public class AnswerRepository extends Repository<Answer> {

    public List<Answer> getAvailableAnswersForUser(User user) {
        String queryTemplate = "SELECT * FROM answers ans " +
                "WHERE ans.id NOT IN (SELECT answer_id FROM answer_history ORDER BY answer_date DESC LIMIT 5) " +
                "AND ans.id NOT IN (SELECT answer_id FROM answer_history WHERE user_id = %d ORDER BY answer_date DESC LIMIT 5) " +
                "AND (ans.user_specific IS NULL OR ans.user_specific = %d);";
        return findByQuery(String.format(queryTemplate, user.getId(), user.getId()));
    }

    public List<Answer> getAvailableAnswersForUserSmart(User user, int limit) {
        String queryTemplate = "SELECT * FROM answers ans " +
                "WHERE ans.id NOT IN (SELECT answer_id FROM answer_history ORDER BY answer_date DESC LIMIT ?) " +
                "AND ans.id NOT IN (SELECT answer_id FROM answer_history WHERE user_id = ? ORDER BY answer_date DESC LIMIT ?) " +
                "AND (ans.user_specific IS NULL OR ans.user_specific = ?);";

        return findByQuery(queryTemplate, limit, user.getId(), limit, user.getId());
    }
}
