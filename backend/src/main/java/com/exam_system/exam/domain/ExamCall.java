package com.exam_system.exam.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_calls")
@Data
public class ExamCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exam_id")
    private Exam exam;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer totalCapacity;
    private Integer currentEnrollment;
}
