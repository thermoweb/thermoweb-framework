package org.thermoweb.discord.database.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.thermoweb.database.annotations.Column;
import org.thermoweb.database.annotations.Entity;
import org.thermoweb.database.annotations.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(table = "answers")
public class Answer {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "content")
    private String content;

    @Column(name = "embedded_img")
    private String embeddedImg;
}
