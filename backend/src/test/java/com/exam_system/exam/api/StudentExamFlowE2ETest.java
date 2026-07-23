package com.exam_system.exam.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.bootstrap.seed-local-test-users=true",
                "app.bootstrap.professor-username=profesor",
                "app.bootstrap.professor-password=profesor123",
                "app.bootstrap.student-username=estudiante",
                "app.bootstrap.student-password=estudiante123"
        }
)
class StudentExamFlowE2ETest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient client;

    @BeforeEach
    void setUp() {
        client = HttpClient.newHttpClient();
    }

    @Test
    void studentCanSeeOpenExamsAndHistoryAndSubmitAttempt() throws Exception {
        String accessToken = loginAsSeededStudent();

        HttpResponse<String> historyBeforeResponse = send(authedRequest(accessToken, "/api/student/attempts").GET().build());
        assertEquals(200, historyBeforeResponse.statusCode());
        JsonNode historyBefore = objectMapper.readTree(historyBeforeResponse.body());
        assertTrue(hasStatus(historyBefore, "GRADED"));

        HttpResponse<String> availableResponse = send(authedRequest(accessToken, "/api/student/exams/available").GET().build());
        assertEquals(200, availableResponse.statusCode());
        JsonNode availableExams = objectMapper.readTree(availableResponse.body());

        JsonNode openExam = findExamWithoutAttempt(availableExams);
        assertFalse(openExam.isMissingNode());
        long examCallId = openExam.path("examCallId").asLong();
        assertTrue(examCallId > 0);

        HttpResponse<String> startResponse = send(
                authedRequest(accessToken, "/api/student/exams/" + examCallId + "/attempts")
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .header("Content-Type", "application/json")
                        .build()
        );
        assertEquals(200, startResponse.statusCode());

        JsonNode startedAttempt = objectMapper.readTree(startResponse.body());
        long attemptId = startedAttempt.path("attemptId").asLong();
        JsonNode questions = startedAttempt.path("questions");
        assertTrue(attemptId > 0);
        assertTrue(questions.isArray());
        assertTrue(questions.size() > 0);

        String answersPayload = buildAnswersPayload((ArrayNode) questions).toString();

        HttpResponse<String> saveResponse = send(
                authedRequest(accessToken, "/api/student/attempts/" + attemptId + "/answers")
                        .PUT(HttpRequest.BodyPublishers.ofString(answersPayload))
                        .header("Content-Type", "application/json")
                        .build()
        );
        assertEquals(200, saveResponse.statusCode());

        HttpResponse<String> submitResponse = send(
                authedRequest(accessToken, "/api/student/attempts/" + attemptId + "/submit")
                        .POST(HttpRequest.BodyPublishers.ofString(answersPayload))
                        .header("Content-Type", "application/json")
                        .build()
        );
        assertEquals(200, submitResponse.statusCode());
        JsonNode submitted = objectMapper.readTree(submitResponse.body());
        assertEquals("SUBMITTED", submitted.path("status").asText());

        HttpResponse<String> historyAfterResponse = send(authedRequest(accessToken, "/api/student/attempts").GET().build());
        assertEquals(200, historyAfterResponse.statusCode());
        JsonNode historyAfter = objectMapper.readTree(historyAfterResponse.body());

        assertTrue(hasAttemptWithStatus(historyAfter, attemptId, "SUBMITTED"));
        assertTrue(hasStatus(historyAfter, "GRADED"));
    }

    private String loginAsSeededStudent() throws IOException, InterruptedException {
        String payload = """
                {
                  "username": "estudiante",
                  "password": "estudiante123"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = send(request);
        assertEquals(200, response.statusCode());

        String token = objectMapper.readTree(response.body()).path("accessToken").asText();
        assertFalse(token.isBlank());
        return token;
    }

    private HttpRequest.Builder authedRequest(String accessToken, String path) {
        return HttpRequest.newBuilder()
                .uri(uri(path))
                .header("Authorization", "Bearer " + accessToken);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode findExamWithoutAttempt(JsonNode availableExams) {
        for (JsonNode exam : availableExams) {
            if (exam.path("attemptId").isNull()) {
                return exam;
            }
        }
        return objectMapper.getNodeFactory().missingNode();
    }

    private ObjectNode buildAnswersPayload(ArrayNode questions) {
        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode answers = objectMapper.createArrayNode();

        int index = 1;
        for (JsonNode question : questions) {
            ObjectNode answer = objectMapper.createObjectNode();
            answer.put("attemptQuestionId", question.path("attemptQuestionId").asLong());
            answer.put("answerText", "Respuesta E2E " + index++);
            answers.add(answer);
        }

        payload.set("answers", answers);
        return payload;
    }

    private boolean hasStatus(JsonNode attempts, String status) {
        for (JsonNode attempt : attempts) {
            if (status.equals(attempt.path("status").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAttemptWithStatus(JsonNode attempts, long attemptId, String status) {
        for (JsonNode attempt : attempts) {
            if (attempt.path("attemptId").asLong() == attemptId && status.equals(attempt.path("status").asText())) {
                return true;
            }
        }
        return false;
    }
}
