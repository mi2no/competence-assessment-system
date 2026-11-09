package org.wildfly.examples.artifact;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "artifact")
public class Artifact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    public LocalDate date;

    public String description;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(nullable = false, unique = true, length = 16)
    public UUID uuid;

    @Column(name = "user_id")
    public int userId;

    public Artifact() {}

    public Artifact(int id, LocalDate date, String description, UUID uuid, int userId) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.uuid = uuid;
        this.userId = userId;
    }
}
