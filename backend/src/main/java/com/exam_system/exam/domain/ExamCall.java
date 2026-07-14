package com.exam_system.exam.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_calls")
@Getter
@Setter
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
