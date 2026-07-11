package com.exam_system.exam.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "attempt_questions")
@Data
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamAttempt getAttempt() {
        return attempt;
    }

    public void setAttempt(ExamAttempt attempt) {
        this.attempt = attempt;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public BigDecimal getAwardedScore() {
        return awardedScore;
    }

    public void setAwardedScore(BigDecimal awardedScore) {
        this.awardedScore = awardedScore;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
}
