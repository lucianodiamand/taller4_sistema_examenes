package com.exam_system.exam.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "questions")
@Getter
@Setter
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @NotBlank(message = "El enunciado es obligatorio")
    @Column(columnDefinition = "TEXT")
    private String statement;

    @NotNull(message = "El tipo de pregunta es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private QuestionType type;

    @NotNull(message = "Los puntos son obligatorios")
    @Min(value = 0, message = "Los puntos no pueden ser negativos")
    private Integer points;
}
