package org.thermoweb.discord.service;

import org.thermoweb.discord.database.model.Answer;
import org.thermoweb.discord.database.model.AnswerHistory;
import org.thermoweb.discord.database.model.Channel;
import org.thermoweb.discord.database.model.User;
import org.thermoweb.discord.database.repository.AnswerHistoryRepository;
import org.thermoweb.discord.database.repository.AnswerRepository;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class AnswerService {

    private final AnswerRepository answerRepository;
    private final AnswerHistoryRepository answerHistoryRepository;
    private final Random rand = new Random();

    public AnswerService() {
        answerRepository = new AnswerRepository();
        answerHistoryRepository = new AnswerHistoryRepository();
    }

    public Optional<Answer> getRandomAnswerForUser(User user) {
        return getRandomAnswerForUser(user, 5);
    }

    public Optional<Answer> getRandomAnswerForUser(User user, int limit) {
        List<Answer> answers = answerRepository.getAvailableAnswersForUserSmart(user, limit);
        int answerId = rand.nextInt(answers.size());

        return Optional.ofNullable(answers.get(answerId));
    }

    public String createAnswerForUser(Answer answer, User user) {
        return answer.getContent()
                .replaceAll("\\{user#id}", user.getCode());
    }

    public void addAnswerHistory(Answer answer, User user, Channel channel, String originalMessage) {
        AnswerHistory answerHistory = AnswerHistory.builder()
                .answer(answer)
                .datetime(Instant.now())
                .user(user)
                .channel(channel)
                .originalMessage(originalMessage)
                .build();
        try {
            answerHistoryRepository.save(answerHistory);
        } catch (SQLException | IllegalAccessException throwables) {
            throwables.printStackTrace();
        }
    }
}
