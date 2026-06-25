package com.exam_system.exam.domain;

import com.exam_system.user.domain.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Table(name = "exams")
@Data
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El titulo es obligatorio")
    private String title;

    private String description;

    @NotNull(message = "La duracion es obligatoria")
    @Min(value = 1, message = "La duracion debe ser al menos de 5 minutos")
    private Integer durationMinutes;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professor_id", nullable = false)
    private User professor;
}
