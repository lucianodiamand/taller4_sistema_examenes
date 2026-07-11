package com.exam_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExamSystemApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	void protectedEndpointWithoutTokenReturnsUnauthorized() throws IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + "/api/exams"))
				.GET()
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals(401, response.statusCode());
	}

}
