package com.exam_system.exam.api.student;

import com.exam_system.exam.application.StudentExamService;
import com.exam_system.exam.application.StudentExamService.AttemptDetailView;
import com.exam_system.exam.application.StudentExamService.AttemptSummaryView;
import com.exam_system.exam.application.StudentExamService.AvailableExamView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints que usa el estudiante para rendir. El id del estudiante nunca se
 * recibe por parámetro: siempre se obtiene del JWT de la sesión actual.
 */
@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT') and hasAuthority('exams.solve')")
public class StudentExamController {

    private final StudentExamService studentExamService;

    public StudentExamController(StudentExamService studentExamService) {
        this.studentExamService = studentExamService;
    }

    @GetMapping("/exams/available")
    public List<AvailableExamView> findAvailableExams() {
        return studentExamService.findAvailableExams();
    }

    @PostMapping("/exams/{examCallId}/attempts")
    public AttemptDetailView startAttempt(@PathVariable Long examCallId) {
        return studentExamService.startAttempt(examCallId);
    }

    @PutMapping("/attempts/{attemptId}/answers")
    public AttemptDetailView saveAnswers(@PathVariable Long attemptId,
                                         @Valid @RequestBody AnswersRequest request) {
        return studentExamService.saveAnswers(attemptId, request.toCommands());
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public AttemptDetailView submitAttempt(@PathVariable Long attemptId,
                                           @Valid @RequestBody AnswersRequest request) {
        return studentExamService.submitAttempt(attemptId, request.toCommands());
    }

    @GetMapping("/attempts")
    public List<AttemptSummaryView> findMyAttempts() {
        return studentExamService.findMyAttempts();
    }

    @GetMapping("/attempts/{attemptId}")
    public AttemptDetailView findMyAttempt(@PathVariable Long attemptId) {
        return studentExamService.findMyAttempt(attemptId);
    }

    public record AnswersRequest(
            @NotNull(message = "La lista de respuestas es obligatoria")
            @Size(max = 200, message = "No se pueden enviar más de 200 respuestas por vez")
            List<@Valid AnswerRequest> answers
    ) {
        private List<StudentExamService.AnswerCommand> toCommands() {
            return answers.stream()
                    .map(answer -> new StudentExamService.AnswerCommand(
                            answer.attemptQuestionId(),
                            answer.answerText()
                    ))
                    .toList();
        }
    }

    public record AnswerRequest(
            @NotNull(message = "El identificador de la pregunta es obligatorio")
            Long attemptQuestionId,
            @Size(max = 10000, message = "La respuesta es demasiado extensa")
            String answerText
    ) {
    }
}
