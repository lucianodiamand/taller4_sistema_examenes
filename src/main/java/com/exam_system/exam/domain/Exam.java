package com.exam_system.exam.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "exams")
@Data
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private Integer durationMinutes;
}
