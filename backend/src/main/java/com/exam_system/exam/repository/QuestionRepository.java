package com.exam_system.exam.repository;

import com.exam_system.exam.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a las preguntas de un examen. El orden por id se usa como base
 * antes de generar el orden aleatorio que recibe cada estudiante.
 */
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByExamIdOrderByIdAsc(Long examId);
}
