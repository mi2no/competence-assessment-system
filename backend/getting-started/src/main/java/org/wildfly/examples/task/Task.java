package org.wildfly.examples.task;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Entity
@Table(name = "task")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @NotBlank
    public String title;

    @NotBlank
    public String description;

    @NotNull
    @Column(name = "creation_date")
    public LocalDate creationDate;

    @NotNull
    @Column(name = "due_date")
    public LocalDate dueDate;

    @NotNull
    @Column(name = "teacher_id")
    public Integer teacherId;

    @NotNull
    @Column(name = "subject_id")
    public Integer subjectId;

    public void createArchive(List<InputPart> files, Path destination) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destination))) {

        }
    }
}
