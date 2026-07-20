package com.exam_system.exam.api;

import com.exam_system.exam.application.ExamService;
import com.exam_system.exam.domain.Exam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('users.read.self')")
    public List<ExamResponse> findAll() {
        return examService.findAll().stream()
                .map(exam -> new ExamResponse(
                        exam.getId(),
                        exam.getTitle(),
                        exam.getDescription(),
                        exam.getDurationMinutes(),
                        exam.getProfessor() == null ? null : exam.getProfessor().getId()
                ))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('exams.create')")
    public ExamResponse create(@Valid @RequestBody CreateExamRequest request) {
        Exam savedExam = examService.create(
                request.title(),
                request.description(),
                request.durationMinutes(),
                request.professorId()
        );

        return new ExamResponse(
                savedExam.getId(),
                savedExam.getTitle(),
                savedExam.getDescription(),
                savedExam.getDurationMinutes(),
                savedExam.getProfessor().getId()
        );
    }

    public record CreateExamRequest(
            @NotBlank(message = "Title is required") String title,
            String description,
            @NotNull(message = "Duration is required")
            @Min(value = 1, message = "Duration must be at least 1")
            Integer durationMinutes,
            @NotNull(message = "Professor is required") Long professorId
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
}
