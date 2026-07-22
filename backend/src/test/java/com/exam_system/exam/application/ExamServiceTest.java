package com.exam_system.exam.application;

import com.exam_system.exam.domain.Exam;
import com.exam_system.exam.repository.ExamRepository;
import com.exam_system.user.domain.User;
import com.exam_system.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private ExamService examService;

    @Test
    void createExamPersistsWithProfessor() {
        User professor = new User();
        professor.setId(5L);

        when(userRepository.findById(5L)).thenReturn(Optional.of(professor));
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Exam created = examService.create("Math", "Midterm", 60, 5L);

        assertEquals("Math", created.getTitle());
        assertEquals(60, created.getDurationMinutes());
        assertEquals(5L, created.getProfessor().getId());
    }

    @Test
    void createExamWithoutProfessorFailsFast() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> examService.create("Math", "Midterm", 60, 999L));
    }

    @Test
    void findAllForProfessorReturnsOnlyOwnExams() {
        Exam exam = new Exam();
        exam.setTitle("Math");

        when(examRepository.findByProfessorId(5L)).thenReturn(List.of(exam));

        List<Exam> result = examService.findAllForProfessor(5L);

        assertEquals(1, result.size());
        assertEquals("Math", result.get(0).getTitle());
    }
}
