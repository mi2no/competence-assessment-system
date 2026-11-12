package org.wildfly.examples.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public LocalDate creationDate;

    @NotNull
    @Column(name = "due_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public LocalDate dueDate;

    @NotNull
    @Column(name = "teacher_id")
    public Integer teacherId;

    @NotNull
    @Column(name = "course_id")
    public Integer courseId;
}
