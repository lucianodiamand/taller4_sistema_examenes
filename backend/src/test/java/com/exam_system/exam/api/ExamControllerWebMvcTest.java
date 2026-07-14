package com.exam_system.exam.api;

import com.exam_system.auth.security.JwtAuthenticationFilter;
import com.exam_system.exam.application.ExamService;
import com.exam_system.exam.domain.Exam;
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

import java.util.List;

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
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(authorities = "users.read.self")
    void getExamsReturnsJsonList() throws Exception {
        User professor = new User();
        professor.setId(77L);

        Exam exam = new Exam();
        exam.setTitle("Algebra");
        exam.setDescription("Parcial 1");
        exam.setDurationMinutes(90);
        exam.setProfessor(professor);

        when(examService.findAll()).thenReturn(List.of(exam));

        mockMvc.perform(get("/api/exams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Algebra"))
                .andExpect(jsonPath("$[0].professorId").value(77));
    }

    @Test
    @WithMockUser(authorities = "exams.create")
    void postExamValidationReturnsBadRequestShape() throws Exception {
        String payload = """
                {
                  "title": "",
                  "description": "x",
                  "durationMinutes": 0,
                  "professorId": null
                }
                """;

        mockMvc.perform(post("/api/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
