package com.exam_system.exam.api;

import com.exam_system.auth.security.CurrentUser;
import com.exam_system.auth.security.JwtAuthenticationFilter;
import com.exam_system.exam.application.ExamService;
import com.exam_system.exam.domain.Exam;
import com.exam_system.exam.domain.ExamCall;
import com.exam_system.exam.domain.Question;
import com.exam_system.exam.domain.QuestionType;
import com.exam_system.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExamController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.exam_system.shared.api.ApiExceptionHandler.class)
class ExamControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExamService examService;

    @MockitoBean
    private CurrentUser currentUser;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(authorities = {"ROLE_PROFESSOR", "exams.create"})
    void getExamsReturnsOnlyAuthenticatedProfessorsExams() throws Exception {
        when(currentUser.id()).thenReturn(77L);

        User professor = new User();
        professor.setId(77L);

        Exam exam = new Exam();
        exam.setTitle("Algebra");
        exam.setDescription("Parcial 1");
        exam.setDurationMinutes(90);
        exam.setProfessor(professor);

        when(examService.findAllForProfessor(77L)).thenReturn(List.of(exam));

        mockMvc.perform(get("/api/exams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Algebra"))
                .andExpect(jsonPath("$[0].professorId").value(77));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_PROFESSOR", "exams.create"})
    void getExamCallsReturnsCallsForOwnedExam() throws Exception {
        when(currentUser.id()).thenReturn(77L);

        Exam exam = new Exam();

        ExamCall call = new ExamCall();
        call.setExam(exam);
        call.setStartDate(LocalDateTime.of(2026, 8, 15, 9, 0));
        call.setEndDate(LocalDateTime.of(2026, 8, 15, 11, 0));
        call.setTotalCapacity(40);
        call.setCurrentEnrollment(12);

        when(examService.findCallsForExam(1L, 77L)).thenReturn(List.of(call));

        mockMvc.perform(get("/api/exams/1/calls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalCapacity").value(40))
                .andExpect(jsonPath("$[0].currentEnrollment").value(12));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_PROFESSOR", "exams.create"})
    void postExamValidationReturnsBadRequestShape() throws Exception {
        String payload = """
                {
                  "title": "",
                  "description": "x",
                  "durationMinutes": 0
                }
                """;

        mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_PROFESSOR", "exams.create"})
    void postExamUsesProfessorIdFromJwtNotRequestBody() throws Exception {
        when(currentUser.id()).thenReturn(77L);

        User professor = new User();
        professor.setId(77L);

        Exam savedExam = new Exam();
        savedExam.setTitle("Algebra");
        savedExam.setDescription("Parcial 1");
        savedExam.setDurationMinutes(90);
        savedExam.setProfessor(professor);

        Question savedQuestion = new Question();
        savedQuestion.setId(10L);
        savedQuestion.setStatement("2+2?");
        savedQuestion.setType(QuestionType.OPEN);
        savedQuestion.setPoints(10);

        when(examService.create(eq("Algebra"), eq("Parcial 1"), eq(90), eq(77L), anyList()))
                .thenReturn(new ExamService.CreationResult(savedExam, List.of(savedQuestion)));

        String payload = """
                {
                  "title": "Algebra",
                  "description": "Parcial 1",
                  "durationMinutes": 90,
                  "questions": [
                    { "statement": "2+2?", "type": "OPEN", "points": 10 }
                  ]
                }
                """;

        mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.professorId").value(77))
                .andExpect(jsonPath("$.questions[0].statement").value("2+2?"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_PROFESSOR", "exams.create"})
    void postExamCallCreatesCallForOwnExam() throws Exception {
        when(currentUser.id()).thenReturn(77L);

        Exam exam = new Exam();
        ExamCall savedCall = new ExamCall();
        savedCall.setExam(exam);
        savedCall.setStartDate(LocalDateTime.of(2026, 8, 1, 9, 0));
        savedCall.setEndDate(LocalDateTime.of(2026, 8, 1, 11, 0));
        savedCall.setTotalCapacity(30);
        savedCall.setCurrentEnrollment(0);

        when(examService.createCall(eq(1L), eq(77L), eq(savedCall.getStartDate()), eq(savedCall.getEndDate()), eq(30)))
                .thenReturn(savedCall);

        String payload = """
                {
                  "startDate": "2026-08-01T09:00:00",
                  "endDate": "2026-08-01T11:00:00",
                  "totalCapacity": 30
                }
                """;

        mockMvc.perform(post("/api/exams/1/calls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCapacity").value(30))
                .andExpect(jsonPath("$.currentEnrollment").value(0));
    }

    // totalCapacity es opcional: null significa cupo ilimitado
    // (ver StudentExamService.hasCapacity, que trata null como sin limite).
    @Test
    @WithMockUser(authorities = {"ROLE_PROFESSOR", "exams.create"})
    void postExamCallAllowsNullTotalCapacityForUnlimitedSeats() throws Exception {
        when(currentUser.id()).thenReturn(77L);

        Exam exam = new Exam();
        ExamCall savedCall = new ExamCall();
        savedCall.setExam(exam);
        savedCall.setStartDate(LocalDateTime.of(2026, 8, 1, 9, 0));
        savedCall.setEndDate(LocalDateTime.of(2026, 8, 1, 11, 0));
        savedCall.setTotalCapacity(null);
        savedCall.setCurrentEnrollment(0);

        when(examService.createCall(eq(1L), eq(77L), eq(savedCall.getStartDate()), eq(savedCall.getEndDate()), eq(null)))
                .thenReturn(savedCall);

        String payload = """
                {
                  "startDate": "2026-08-01T09:00:00",
                  "endDate": "2026-08-01T11:00:00"
                }
                """;

        mockMvc.perform(post("/api/exams/1/calls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCapacity").isEmpty());
    }

}
