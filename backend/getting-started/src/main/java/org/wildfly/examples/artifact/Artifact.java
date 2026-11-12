package org.wildfly.examples.artifact;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wildfly.examples.task.Task;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "artifact")
public class Artifact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public LocalDate date;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public String description;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(nullable = false, unique = true, length = 16)
    public UUID uuid;

    @Column(name = "user_id")
    public int userId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    public Task task;

    public Artifact() {}

    public Artifact(int id, LocalDate date, String description, UUID uuid, int userId, Task task) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.uuid = uuid;
        this.userId = userId;
        this.task = task;
    }
}
