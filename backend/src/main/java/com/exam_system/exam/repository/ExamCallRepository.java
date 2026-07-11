package com.exam_system.exam.repository;

import com.exam_system.exam.domain.ExamCall;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamCallRepository extends JpaRepository<ExamCall, Long> {
}
