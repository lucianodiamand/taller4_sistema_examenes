package com.exam_system.exam.repository;

import com.exam_system.exam.domain.ExamAttempt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    // Profesor: todas las resoluciones de los exámenes que le pertenecen
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.examCall.exam.professor.id = :professorId")
    List<ExamAttempt> findAllByProfessorId(@Param("professorId") Long professorId);

    // Profesor: resoluciones de un examen específico, verificando que sea suyo
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.examCall.exam.id = :examId AND ea.examCall.exam.professor.id = :professorId")
    List<ExamAttempt> findAllByExamIdAndProfessorId(@Param("examId") Long examId, @Param("professorId") Long professorId);

    /**
     * Intento puntual verificando que pertenezca a uno de los exámenes del profesor.
     * Si el intento corresponde a otro profesor, se comporta como inexistente (404, no 403).
     */
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.id = :attemptId AND ea.examCall.exam.professor.id = :professorId")
    Optional<ExamAttempt> findByIdAndProfessorId(@Param("attemptId") Long attemptId, @Param("professorId") Long professorId);

    /**
     * Todas las resoluciones del estudiante, ordenadas de más reciente a más antigua.
     */
    @EntityGraph(attributePaths = {
            "examCall",
            "examCall.exam",
            "examCall.exam.professor",
            "questions",
            "questions.question"
    })
    List<ExamAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);

    // Estudiante: todas sus resoluciones sin orden garantizado
    List<ExamAttempt> findByStudentId(Long studentId);

    // Estudiante: resoluciones filtrando por un conjunto de convocatorias
    List<ExamAttempt> findByStudentIdAndExamCallIdIn(Long studentId, Collection<Long> examCallIds);

    /**
     * El studentId forma parte de la consulta a propósito: así una resolución ajena
     * se comporta como inexistente y nunca llega a exponerse al estudiante.
     */
    @EntityGraph(attributePaths = {
            "examCall",
            "examCall.exam",
            "examCall.exam.professor",
            "questions",
            "questions.question"
    })
    Optional<ExamAttempt> findByIdAndStudentId(Long attemptId, Long studentId);

    // Verificación rápida de duplicado al momento de iniciar un intento
    Optional<ExamAttempt> findByExamCallIdAndStudentId(Long examCallId, Long studentId);
}
