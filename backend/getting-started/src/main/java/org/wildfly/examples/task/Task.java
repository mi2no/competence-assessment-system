package org.wildfly.examples.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

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
    public LocalDateTime creationDate;

    @NotNull
    @Column(name = "due_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public LocalDateTime dueDate;

    @NotNull
    @Column(name = "teacher_id")
    public Integer teacherId;

    @NotNull
    @Column(name = "course_id")
    public Integer courseId;
}
