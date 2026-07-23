package com.exam_system.exam.repository;

import com.exam_system.exam.domain.ExamCall;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExamCallRepository extends JpaRepository<ExamCall, Long> {

    @EntityGraph(attributePaths = {"exam"})
    List<ExamCall> findByExamIdOrderByStartDateDesc(Long examId);

    /**
     * Trae únicamente convocatorias abiertas y deja cargados los datos del examen
     * que necesita el listado del estudiante.
     */
    @EntityGraph(attributePaths = {"exam", "exam.professor"})
    @Query("""
            select examCall
            from ExamCall examCall
            where examCall.startDate <= :now
              and examCall.endDate >= :now
            order by examCall.endDate asc
            """)
    List<ExamCall> findOpenCalls(@Param("now") LocalDateTime now);

    /**
     * Bloquea la convocatoria mientras se crea un intento para que dos altas
     * simultáneas no superen accidentalmente el cupo disponible.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"exam", "exam.professor"})
    @Query("select examCall from ExamCall examCall where examCall.id = :id")
    Optional<ExamCall> findByIdForUpdate(@Param("id") Long id);
}
