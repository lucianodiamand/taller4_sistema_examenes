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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExamService {

    private static final Logger logger = LoggerFactory.getLogger(ExamService.class);

    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final ExamCallRepository examCallRepository;

    public ExamService(ExamRepository examRepository, UserRepository userRepository,
                        QuestionRepository questionRepository, ExamCallRepository examCallRepository) {
        this.examRepository = examRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.examCallRepository = examCallRepository;
    }

    @Transactional(readOnly = true)
    public List<Exam> findAllForProfessor(Long professorId) {
        logger.info("Fetching exam list for professor {}", professorId);
        return examRepository.findByProfessorId(professorId);
    }

    @Transactional
    public CreationResult create(String title, String description, Integer durationMinutes, Long professorId,
                                  List<QuestionInput> questions) {
        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found"));

        Exam exam = new Exam();
        exam.setTitle(title);
        exam.setDescription(description);
        exam.setDurationMinutes(durationMinutes);
        exam.setProfessor(professor);

        Exam savedExam = examRepository.save(exam);

        List<Question> savedQuestions = questions.stream()
                .map(input -> {
                    Question question = new Question();
                    question.setExam(savedExam);
                    question.setStatement(input.statement());
                    question.setType(input.type());
                    question.setPoints(input.points());
                    return questionRepository.save(question);
                })
                .toList();

        logger.info("Created exam {} with {} questions for professor {}",
                savedExam.getId(), savedQuestions.size(), professorId);
        return new CreationResult(savedExam, savedQuestions);
    }

    @Transactional
    public ExamCall createCall(Long examId, Long professorId, LocalDateTime startDate, LocalDateTime endDate,
                                Integer totalCapacity) {
        Exam exam = examRepository.findByIdAndProfessorId(examId, professorId)
                .orElseThrow(() -> new EntityNotFoundException("El examen no existe o no te pertenece"));

        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }

        ExamCall examCall = new ExamCall();
        examCall.setExam(exam);
        examCall.setStartDate(startDate);
        examCall.setEndDate(endDate);
        examCall.setTotalCapacity(totalCapacity);
        examCall.setCurrentEnrollment(0);

        ExamCall savedCall = examCallRepository.save(examCall);
        logger.info("Created exam call {} for exam {} ({} - {})", savedCall.getId(), examId, startDate, endDate);
        return savedCall;
    }

    public record QuestionInput(String statement, QuestionType type, Integer points) {
    }

    public record CreationResult(Exam exam, List<Question> questions) {
    }
}
