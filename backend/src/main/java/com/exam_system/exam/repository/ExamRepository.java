package com.exam_system.exam.repository;

import com.exam_system.exam.domain.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findByProfessorId(Long professorId);

    Optional<Exam> findByIdAndProfessorId(Long id, Long professorId);
}
