package org.thermoweb.discord.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.thermoweb.database.annotations.Column;
import org.thermoweb.database.annotations.Entity;
import org.thermoweb.database.annotations.Id;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(table = "answer_history")
public class AnswerHistory {

    @Id
    @Column
    private Integer id;

    @Column(name = "user_id")
    private User user;

    @Column(name = "answer_id")
    private Answer answer;

    @Column(name = "answer_date")
    private LocalDateTime datetime;
}
