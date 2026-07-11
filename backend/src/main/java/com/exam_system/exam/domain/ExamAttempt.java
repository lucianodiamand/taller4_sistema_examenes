package com.exam_system.exam.domain;

import com.exam_system.user.domain.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "exam_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exam_attempt_call_student",
                columnNames = {"exam_call_id", "student_id"}
        )
)
@Data
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La convocatoria es obligatoria")
    @ManyToOne(optional = false)
    @JoinColumn(name = "exam_call_id", nullable = false)
    private ExamCall examCall;

    @NotNull(message = "El estudiante es obligatorio")
    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    @Column(nullable = false, length = 30)
    private String status = "IN_PROGRESS";

    @DecimalMin(value = "0.0", message = "La nota final no puede ser negativa")
    private BigDecimal finalScore;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    private List<AttemptQuestion> questions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamCall getExamCall() {
        return examCall;
    }

    public void setExamCall(ExamCall examCall) {
        this.examCall = examCall;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public List<AttemptQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<AttemptQuestion> questions) {
        this.questions = questions;
    }
}
