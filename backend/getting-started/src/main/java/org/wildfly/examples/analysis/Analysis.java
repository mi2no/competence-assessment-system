package org.wildfly.examples.analysis;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.wildfly.examples.artifact.Artifact;

import java.time.LocalDateTime;

@Entity(name = "analysis")
public class Analysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artifact_id")
    private Artifact artifact;

    public Analysis() {}

    public Analysis(String description, LocalDateTime creationDate, Artifact artifact) {
        this.description = description;
        this.creationDate = creationDate;
        this.artifact = artifact;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }
}
