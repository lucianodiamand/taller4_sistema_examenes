package com.exam_system.exam.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "attempt_questions")
@Getter
@Setter
public class AttemptQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El intento es obligatorio")
    @ManyToOne(optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @NotNull(message = "La pregunta es obligatoria")
    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @NotNull(message = "El orden de la pregunta es obligatorio")
    @Min(value = 1, message = "El orden debe comenzar en 1")
    private Integer questionOrder;

    @Column(columnDefinition = "TEXT")
    private String answerText;

    @DecimalMin(value = "0.0", message = "El puntaje asignado no puede ser negativo")
    private BigDecimal awardedScore;

    @Column(columnDefinition = "TEXT")
    private String reviewComment;
}
