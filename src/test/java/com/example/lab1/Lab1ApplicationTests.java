package com.example.lab1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class Lab1ApplicationTests {
    private static ConfigurableApplicationContext context;
    private static HttpClient client;
    private static String base;

    @BeforeAll
    static void start() {
        context = SpringApplication.run(Lab1Application.class, "--server.port=0");
        base = "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port") + "/api/";
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterAll
    static void stop() {
        if (context != null) context.close();
    }

    private HttpResponse<String> call(String method, String path, String body, int expected) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(expected, response.statusCode(), method + " " + path + ": " + response.body());
        return response;
    }

    @Test
    void crudValidationAndCascades() throws Exception {
        List<String> resources = List.of("users", "podcasts", "episodes", "audio-files", "publication-slots");
        for (String resource : resources) {
            assertEquals("[]", call("GET", resource, null, 200).body());
            call("GET", resource + "/999", null, 404);
            call("PATCH", resource + "/999", "{}", 404);
            call("DELETE", resource + "/999", null, 404);
            call("POST", resource, "{}", 400);
        }
        call("POST", "users", "{\"name\":\"Alice\",\"email\":\"alice@example.com\",\"createdAt\":\"2020-01-01T00:00:00Z\"}", 400);
        call("POST", "users", "{\"name\":\"Alice\",\"email\":\"alice@example.com\",\"id\":99}", 400);
        call("POST", "users", "{\"name\":\" \",\"email\":\"bad\"}", 400);
        HttpResponse<String> user = call("POST", "users", "{\"name\":\"Alice\",\"email\":\"alice@example.com\"}", 201);
        assertTrue(user.body().contains("\"id\":1"));
        assertTrue(user.body().contains("\"createdAt\":"));
        assertEquals("/api/users/1", user.headers().firstValue("Location").orElseThrow());
        call("POST", "users", "{\"name\":\"Other\",\"email\":\"ALICE@example.com\"}", 409);
        call("POST", "podcasts", "{\"title\":\"Podcast\",\"authorId\":999,\"status\":\"ACTIVE\"}", 404);
        call("POST", "podcasts", "{\"title\":\"Podcast\",\"authorId\":1,\"status\":\"INVALID\"}", 400);
        call("POST", "podcasts", "{\"title\":\"Podcast\",\"authorId\":1,\"status\":\"ACTIVE\"}", 201);
        call("POST", "episodes", "{\"podcastId\":1,\"title\":\"Episode\",\"durationSeconds\":60,\"publicationState\":\"DRAFT\"}", 201);
        call("POST", "audio-files", "{\"episodeId\":1,\"path\":\"audio/1.mp3\",\"format\":\"mp3\",\"sizeBytes\":1000,\"primary\":true}", 201);
        call("POST", "audio-files", "{\"episodeId\":1,\"path\":\"audio/2.mp3\",\"format\":\"mp3\",\"sizeBytes\":1000,\"primary\":true}", 409);
        call("POST", "publication-slots", "{\"podcastId\":1,\"episodeId\":1,\"scheduledAt\":\"2026-12-01T12:00:00+03:00\",\"status\":\"SCHEDULED\"}", 201);
        Map<String, String> patches = Map.of(
                "users", "{\"name\":\"Bob\"}",
                "podcasts", "{\"title\":\"Updated podcast\"}",
                "episodes", "{\"title\":\"Updated episode\"}",
                "audio-files", "{\"primary\":false}",
                "publication-slots", "{\"status\":\"CANCELLED\"}");
        for (String resource : resources) {
            String before = call("GET", resource + "/1", null, 200).body();
            String after = call("PATCH", resource + "/1", patches.get(resource), 200).body();
            assertNotEquals(before, after);
            assertTrue(after.contains("\"id\":1"));
            assertTrue(call("GET", resource, null, 200).body().contains("\"id\":1"));
        }
        assertTrue(call("GET", "users/1", null, 200).body().contains("alice@example.com"));
        call("PATCH", "episodes/1", "{\"durationSeconds\":-1}", 400);
        call("PATCH", "audio-files/1", "{\"sizeBytes\":0}", 400);
        call("PATCH", "publication-slots/1", "{\"updatedAt\":\"2020-01-01T00:00:00Z\"}", 400);
        call("POST", "podcasts", "{\"title\":\"Second\",\"authorId\":1,\"status\":\"DRAFT\"}", 201);
        call("PATCH", "publication-slots/1", "{\"podcastId\":2}", 400);
        call("PATCH", "episodes/1", "{\"podcastId\":2}", 409);
        assertEquals("", call("DELETE", "publication-slots/1", null, 204).body());
        assertEquals("", call("DELETE", "audio-files/1", null, 204).body());
        call("DELETE", "episodes/1", null, 204);
        call("DELETE", "podcasts/2", null, 204);
        call("POST", "episodes", "{\"podcastId\":1,\"title\":\"Cascade\",\"durationSeconds\":60,\"publicationState\":\"DRAFT\"}", 201);
        call("POST", "audio-files", "{\"episodeId\":2,\"path\":\"a.wav\",\"format\":\"wav\",\"sizeBytes\":1,\"primary\":true}", 201);
        call("POST", "publication-slots", "{\"podcastId\":1,\"episodeId\":2,\"scheduledAt\":\"2026-12-01T12:00:00Z\",\"status\":\"SCHEDULED\"}", 201);
        call("DELETE", "users/1", null, 204);
        for (String resource : resources) {
            assertEquals("[]", call("GET", resource, null, 200).body(), "Cascade left data in " + resource);
        }
    }

}
