package com.exam_system.exam.application;

import com.exam_system.exam.domain.Exam;
import com.exam_system.exam.repository.ExamRepository;
import com.exam_system.user.domain.User;
import com.exam_system.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExamService {

    private static final Logger logger = LoggerFactory.getLogger(ExamService.class);

    private final ExamRepository examRepository;
    private final UserRepository userRepository;

    public ExamService(ExamRepository examRepository, UserRepository userRepository) {
        this.examRepository = examRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Exam> findAll() {
        logger.info("Fetching exam list");
        return examRepository.findAll();
    }

    @Transactional
    public Exam create(String title, String description, Integer durationMinutes, Long professorId) {
        User professor = userRepository.findById(professorId)
                .orElseThrow(() -> new EntityNotFoundException("Professor not found"));

        Exam exam = new Exam();
        exam.setTitle(title);
        exam.setDescription(description);
        exam.setDurationMinutes(durationMinutes);
        exam.setProfessor(professor);

        Exam savedExam = examRepository.save(exam);
        logger.info("Created exam {} for professor {}", savedExam.getId(), professorId);
        return savedExam;
    }
}
