package com.exam_system.exam.api;

import com.exam_system.auth.security.CurrentUser;
import com.exam_system.exam.application.ExamService;
import com.exam_system.exam.domain.ExamCall;
import com.exam_system.exam.domain.Question;
import com.exam_system.exam.domain.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;
    private final CurrentUser currentUser;

    public ExamController(ExamService examService, CurrentUser currentUser) {
        this.examService = examService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasRole('PROFESSOR') and hasAuthority('exams.create')")
    public List<ExamResponse> findMine() {
        return examService.findAllForProfessor(currentUser.id()).stream()
                .map(exam -> new ExamResponse(
                        exam.getId(),
                        exam.getTitle(),
                        exam.getDescription(),
                        exam.getDurationMinutes(),
                        exam.getProfessor() == null ? null : exam.getProfessor().getId()
                ))
                .toList();
    }

    @GetMapping("/{examId}/calls")
    @PreAuthorize("hasRole('PROFESSOR') and hasAuthority('exams.create')")
    public List<ExamCallResponse> findCalls(@PathVariable Long examId) {
        return examService.findCallsForExam(examId, currentUser.id()).stream()
                .map(ExamCallResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFESSOR') and hasAuthority('exams.create')")
    public ExamCreatedResponse create(@Valid @RequestBody CreateExamRequest request) {
        ExamService.CreationResult result = examService.create(
                request.title(),
                request.description(),
                request.durationMinutes(),
                currentUser.id(),
                request.questions().stream()
                        .map(q -> new ExamService.QuestionInput(q.statement(), q.type(), q.points()))
                        .toList()
        );

        return new ExamCreatedResponse(
                result.exam().getId(),
                result.exam().getTitle(),
                result.exam().getDescription(),
                result.exam().getDurationMinutes(),
                result.exam().getProfessor().getId(),
                result.questions().stream().map(QuestionResponse::from).toList()
        );
    }

    @PostMapping("/{examId}/calls")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFESSOR') and hasAuthority('exams.create')")
    public ExamCallResponse createCall(@PathVariable Long examId, @Valid @RequestBody CreateExamCallRequest request) {
        ExamCall call = examService.createCall(
                examId,
                currentUser.id(),
                request.startDate(),
                request.endDate(),
                request.totalCapacity()
        );

        return ExamCallResponse.from(call);
    }

    public record CreateExamRequest(
            @NotBlank(message = "El titulo es obligatorio") String title,
            @Size(max = 2000, message = "La descripcion no puede superar los 2000 caracteres") String description,
            @NotNull(message = "La duracion es obligatoria")
            @Min(value = 1, message = "La duracion debe ser al menos 1")
            Integer durationMinutes,
            @NotEmpty(message = "El examen debe tener al menos una pregunta")
            List<@Valid QuestionRequest> questions
    ) {
    }

    public record QuestionRequest(
            @NotBlank(message = "El enunciado es obligatorio") String statement,
            @NotNull(message = "El tipo de pregunta es obligatorio") QuestionType type,
            @NotNull(message = "Los puntos son obligatorios")
            @Min(value = 0, message = "Los puntos no pueden ser negativos")
            Integer points
    ) {
    }

    public record ExamResponse(
            Long id,
            String title,
            String description,
            Integer durationMinutes,
            Long professorId
    ) {
    }

    public record QuestionResponse(
            Long id,
            String statement,
            QuestionType type,
            Integer points
    ) {
        static QuestionResponse from(Question question) {
            return new QuestionResponse(
                    question.getId(),
                    question.getStatement(),
                    question.getType(),
                    question.getPoints()
            );
        }
    }

    public record ExamCreatedResponse(
            Long id,
            String title,
            String description,
            Integer durationMinutes,
            Long professorId,
            List<QuestionResponse> questions
    ) {
    }

    public record CreateExamCallRequest(
            @NotNull(message = "La fecha de inicio es obligatoria") LocalDateTime startDate,
            @NotNull(message = "La fecha de fin es obligatoria") LocalDateTime endDate,
            // Null = cupo ilimitado (ver StudentExamService.hasCapacity)
            @Min(value = 1, message = "El cupo debe ser al menos 1")
            Integer totalCapacity
    ) {
    }

    public record ExamCallResponse(
            Long id,
            Long examId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer totalCapacity,
            Integer currentEnrollment
    ) {
        static ExamCallResponse from(ExamCall call) {
            return new ExamCallResponse(
                    call.getId(),
                    call.getExam().getId(),
                    call.getStartDate(),
                    call.getEndDate(),
                    call.getTotalCapacity(),
                    call.getCurrentEnrollment()
            );
        }
    }
}
