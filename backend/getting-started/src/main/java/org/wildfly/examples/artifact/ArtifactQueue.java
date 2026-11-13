package org.wildfly.examples.artifact;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "artifact_queue")
public class ArtifactQueue {
    @Id
    public int artifact_id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "artifact_id")
    @JsonIgnore
    public Artifact artifact;

    public enum State {
        WAITING, IN_PROGRESS, FAILED
    }

    @JdbcTypeCode(SqlTypes.ENUM)
    @Enumerated(EnumType.ORDINAL)
    public State state;

    public ArtifactQueue() {}

    public ArtifactQueue(Artifact artifact, State state) {
        this.artifact = artifact;
        this.state = state;
    }
}