package org.wildfly.examples.competence;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "competence")
public class Competence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @NotBlank
    @Column(nullable = false, name = "concept_uri")
    public String conceptUri;

    @NotBlank
    @Column(nullable = false)
    public String name;

    @NotBlank
    @Column(nullable = false, name = "skill_type")
    public String skillType;

    public String description;

    public Competence() {}

    public Competence(Integer id, String conceptUri, String name, String skillType, String description) {
        this.id = id;
        this.conceptUri = conceptUri;
        this.name = name;
        this.skillType = skillType;
        this.description = description;
    }
}
