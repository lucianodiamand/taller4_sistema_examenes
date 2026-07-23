package com.exam_system.exam.application;

import com.exam_system.exam.domain.Exam;
import com.exam_system.exam.domain.ExamCall;
import com.exam_system.exam.domain.Question;
import com.exam_system.exam.domain.QuestionType;
import com.exam_system.exam.repository.ExamCallRepository;
import com.exam_system.exam.repository.ExamRepository;
import com.exam_system.exam.repository.QuestionRepository;
import com.exam_system.user.domain.User;
import com.exam_system.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ExamCallRepository examCallRepository;

    @InjectMocks
    private ExamService examService;

    private static final List<ExamService.QuestionInput> ONE_QUESTION = List.of(
            new ExamService.QuestionInput("2+2?", QuestionType.OPEN, 10));

    // Caso feliz: arma un profesor con id 5, le dice al mock "si te preguntan
    // por el usuario 5, existe". Llama a create("Math", "Midterm", 60, 5L, ...)
    // con una pregunta de ejemplo ("2+2?", tipo OPEN, 10 puntos). Verifica que
    // lo que vuelve tenga titulo "Math", duracion 60, el profesor 5 como dueño,
    // y esa misma pregunta guardada con su enunciado, tipo y puntos intactos.
    @Test
    void createExamPersistsWithProfessorAndQuestions() {
        User professor = new User();
        professor.setId(5L);

        when(userRepository.findById(5L)).thenReturn(Optional.of(professor));
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamService.CreationResult result = examService.create("Math", "Midterm", 60, 5L, ONE_QUESTION);

        assertEquals("Math", result.exam().getTitle());
        assertEquals(60, result.exam().getDurationMinutes());
        assertEquals(5L, result.exam().getProfessor().getId());
        assertEquals(1, result.questions().size());
        assertEquals("2+2?", result.questions().get(0).getStatement());
        assertEquals(QuestionType.OPEN, result.questions().get(0).getType());
        assertEquals(10, result.questions().get(0).getPoints());
    }

    // Caso de error: el mock dice "el usuario 999 no existe" (Optional.empty()).
    // Llama a create(..., 999L, ...) y verifica que explote con
    // EntityNotFoundException, o sea que el service no intente crear un
    // examen para un profesor fantasma.
    @Test
    void createExamWithoutProfessorFailsFast() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> examService.create("Math", "Midterm", 60, 999L, ONE_QUESTION));
    }

    // Prueba el listado: crea un examen "Math" y le dice al mock "si preguntan
    // por los examenes del profesor 5, devolve este". Llama a
    // findAllForProfessor(5L) y verifica que devuelva justo ese unico examen,
    // que el service no agregue ni filtre nada raro, solo pase lo que el
    // repositorio le dio.
    @Test
    void findAllForProfessorReturnsOnlyOwnExams() {
        Exam exam = new Exam();
        exam.setTitle("Math");

        when(examRepository.findByProfessorId(5L)).thenReturn(List.of(exam));

        List<Exam> result = examService.findAllForProfessor(5L);

        assertEquals(1, result.size());
        assertEquals("Math", result.get(0).getTitle());
    }

    // Caso feliz de convocatoria: el examen 10 existe y pertenece al
    // profesor 5. Verifica que la convocatoria quede asociada a ese examen,
    // con las fechas y el cupo pedidos, y arranque con 0 inscriptos.
    @Test
    void createCallPersistsForOwnedExam() {
        User professor = new User();
        professor.setId(5L);
        Exam exam = new Exam();
        exam.setProfessor(professor);

        when(examRepository.findByIdAndProfessorId(10L, 5L)).thenReturn(Optional.of(exam));
        when(examCallRepository.save(any(ExamCall.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 1, 11, 0);

        ExamCall result = examService.createCall(10L, 5L, start, end, 30);

        assertEquals(exam, result.getExam());
        assertEquals(start, result.getStartDate());
        assertEquals(end, result.getEndDate());
        assertEquals(30, result.getTotalCapacity());
        assertEquals(0, result.getCurrentEnrollment());
    }

    // Caso de ownership: el examen 10 no es del profesor 5 (o no existe),
    // asi que el repositorio scopeado devuelve vacio. Verifica que no se
    // pueda crear una convocatoria para un examen ajeno.
    @Test
    void createCallFailsWhenExamNotOwnedByProfessor() {
        when(examRepository.findByIdAndProfessorId(10L, 5L)).thenReturn(Optional.empty());

        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 1, 11, 0);

        assertThrows(EntityNotFoundException.class,
                () -> examService.createCall(10L, 5L, start, end, 30));
    }

    // Caso de validacion: la fecha de fin no es posterior a la de inicio.
    // Verifica que el service rechace la convocatoria antes de guardarla.
    @Test
    void createCallFailsWhenEndDateNotAfterStartDate() {
        User professor = new User();
        professor.setId(5L);
        Exam exam = new Exam();
        exam.setProfessor(professor);

        when(examRepository.findByIdAndProfessorId(10L, 5L)).thenReturn(Optional.of(exam));

        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 11, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 1, 9, 0);

        assertThrows(IllegalArgumentException.class,
                () -> examService.createCall(10L, 5L, start, end, 30));
    }
}
