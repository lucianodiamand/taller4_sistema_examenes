package com.exam_system.exam.repository;

import com.exam_system.exam.domain.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    Optional<ExamAttempt> findByExamCallIdAndStudentId(Long examCallId, Long studentId);

    List<ExamAttempt> findByStudentId(Long studentId);
}
