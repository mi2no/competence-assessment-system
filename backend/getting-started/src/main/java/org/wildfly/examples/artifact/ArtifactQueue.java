package org.wildfly.examples.artifact;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "artifact_queue")
public class ArtifactQueue {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(nullable = false, unique = true, length = 16)
    public UUID uuid;

    public LocalDate date;

    @Column(name = "user_id")
    public int userId;

    public enum State {
        WAITING, IN_PROGRESS
    }

    @JdbcTypeCode(SqlTypes.ENUM)
    @Enumerated(EnumType.ORDINAL)
    public State state;

    public ArtifactQueue() {}

    public ArtifactQueue(LocalDate date, UUID uuid, int userId, State state) {
        this.date = date;
        this.uuid = uuid;
        this.userId = userId;
        this.state = state;
    }
}