package com.example.lab1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import java.net.URI;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.json.JsonMapper;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class Lab1ApplicationTests {
    private static ConfigurableApplicationContext context;
    private static HttpClient client;
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static String base;
    private static String admin;
    private static String sessionCookie;
    private static String csrfHeader;
    private static String csrfToken;
    private String userPath;

    @BeforeAll
    static void start() throws Exception {
        context = SpringApplication.run(Lab1Application.class, "--server.port=0");
        base = "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port") + "/api/";
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        admin = basic(context.getEnvironment().getRequiredProperty("ADMIN_EMAIL"),
                context.getEnvironment().getRequiredProperty("ADMIN_PASSWORD"));
        var tokenResponse = callAs("GET", "auth/csrf", null, null, false, 200);
        var cookie = HttpCookie.parse(tokenResponse.headers().firstValue("Set-Cookie").orElseThrow()).get(0);
        sessionCookie = cookie.getName() + "=" + cookie.getValue();
        var csrf = JSON.readTree(tokenResponse.body());
        csrfHeader = csrf.get("headerName").asString();
        csrfToken = csrf.get("token").asString();
    }

    @AfterAll
    static void stop() {
        if (context != null) context.close();
    }

    private HttpResponse<String> call(String method, String path, String body, int expected) throws Exception {
        return callAs(method, path, body, admin, true, expected);
    }

    private static String basic(String email, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((email + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> callAs(String method, String path, String body, String auth,
                                                boolean sendCsrf, int expected) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (auth != null) request.header("Authorization", auth);
        if (sessionCookie != null) request.header("Cookie", sessionCookie);
        if (sendCsrf && !method.equals("GET")) request.header(csrfHeader, csrfToken);
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(expected, response.statusCode(), method + " " + path + ": " + response.body());
        return response;
    }

    private String created(String path, String body) throws Exception {
        HttpResponse<String> response = call("POST", path, body, 201);
        return response.headers().firstValue("Location").orElseThrow().substring("/api/".length());
    }

    private static String id(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @org.junit.jupiter.api.AfterEach
    void cleanUp() throws Exception {
        if (userPath != null) call("DELETE", userPath, null, 204);
    }

    @Test
    void crudValidationAndCascades() throws Exception {
        List<String> resources = List.of("users", "podcasts", "episodes", "audio-files", "publication-slots");
        for (String resource : resources) {
            call("GET", resource, null, 200);
            call("GET", resource + "/9223372036854775807", null, 404);
            call("PATCH", resource + "/9223372036854775807", "{}", 404);
            call("DELETE", resource + "/9223372036854775807", null, 404);
            call("POST", resource, "{}", 400);
        }
        call("POST", "users", """
                {"name":"Alice","email":"alice@example.com","createdAt":"2020-01-01T00:00:00Z"}
                """, 400);
        call("POST", "users", """
                {"name":"Alice","email":"alice@example.com","id":99}
                """, 400);
        call("POST", "users", """
                {"name":" ","email":"bad"}
                """, 400);
        String email = UUID.randomUUID() + "@example.test";
        userPath = created("users", """
                {"name":"Alice","email":"%s"}
                """.formatted(email));
        assertTrue(call("GET", userPath, null, 200).body().contains("\"createdAt\":"));
        call("POST", "users", """
                {"name":"Other","email":"%s"}
                """.formatted(email.toUpperCase(Locale.ROOT)), 409);
        call("POST", "podcasts", """
                {"title":"Podcast","authorId":9223372036854775807,"status":"ACTIVE"}
                """, 404);
        call("POST", "podcasts", """
                {"title":"Podcast","authorId":%s,"status":"INVALID"}
                """.formatted(id(userPath)), 400);

        String podcast = created("podcasts", """
                {"title":"Podcast","authorId":%s,"status":"ACTIVE"}
                """.formatted(id(userPath)));
        String otherPodcast = created("podcasts", """
                {"title":"Other podcast","authorId":%s,"status":"DRAFT"}
                """.formatted(id(userPath)));
        String episode = created("episodes", """
                {"podcastId":%s,"title":"Episode","durationSeconds":60,"publicationState":"DRAFT"}
                """.formatted(id(podcast)));
        String audioBody = """
                {"episodeId":%s,"path":"audio/1.mp3","format":"mp3","sizeBytes":1000,"primary":true}
                """.formatted(id(episode));
        String audio = created("audio-files", audioBody);
        call("POST", "audio-files", audioBody, 409);
        String slot = created("publication-slots", """
                {"podcastId":%s,"episodeId":%s,"scheduledAt":"2026-12-01T12:00:00+03:00","status":"SCHEDULED"}
                """.formatted(id(podcast), id(episode)));
        Map<String, String> patches = Map.of(
                userPath, "{\"name\":\"Bob\"}",
                podcast, "{\"title\":\"Updated podcast\"}",
                episode, "{\"title\":\"Updated episode\"}",
                audio, "{\"primary\":false}",
                slot, "{\"status\":\"CANCELLED\"}");
        for (var patch : patches.entrySet()) {
            String path = patch.getKey();
            String before = call("GET", path, null, 200).body();
            String after = call("PATCH", path, patch.getValue(), 200).body();
            assertNotEquals(before, after);
            assertTrue(after.contains("\"id\":" + id(path)));
        }
        assertTrue(call("GET", userPath, null, 200).body().contains(email));
        call("PATCH", episode, "{\"durationSeconds\":-1}", 400);
        call("PATCH", audio, "{\"sizeBytes\":0}", 400);
        call("PATCH", slot, "{\"updatedAt\":\"2020-01-01T00:00:00Z\"}", 400);
        call("PATCH", slot, "{\"podcastId\":" + id(otherPodcast) + "}", 400);
        call("PATCH", episode, "{\"podcastId\":" + id(otherPodcast) + "}", 409);
        assertEquals("", call("DELETE", userPath, null, 204).body());
        for (String path : List.of(userPath, podcast, otherPodcast, episode, audio, slot)) {
            call("GET", path, null, 404);
        }
        userPath = null;
    }

    @Test
    void registrationBasicAuthCsrfAndAccountState() throws Exception {
        assertTrue(callAs("GET", "podcasts", null, null, false, 401)
                .headers().firstValue("WWW-Authenticate").orElseThrow().startsWith("Basic"));
        String email = "security-" + UUID.randomUUID() + "@example.test";
        String password = "Example-Test7!";
        String auth = basic(email, password);
        String registration = """
                {"name":"Test account","email":"%s","password":"%s"}
                """.formatted(email, password);
        assertTrue(callAs("POST", "auth/register", registration, null, false, 403)
                .body().contains("CSRF_INVALID"));
        callAs("POST", "auth/register", registration.replace(password, "weak"), null, true, 400);
        callAs("POST", "auth/register", registration.replace("}", ",\"role\":\"ADMIN\"}"), null, true, 400);
        var registered = JSON.readTree(callAs("POST", "auth/register", registration, null, true, 201).body());
        userPath = "users/" + registered.get("userId").asLong();
        assertEquals("READER", registered.get("role").asString());
        assertFalse(registered.has("password"));
        callAs("POST", "auth/register", registration.replace(email, email.toUpperCase(Locale.ROOT)), null, true, 409);
        callAs("GET", "auth/me", null, basic(email, "wrong"), false, 401);
        callAs("GET", "auth/me", null, basic(email, "x".repeat(100)), false, 401);
        callAs("GET", "auth/me", null, basic(email.toUpperCase(Locale.ROOT), password), false, 200);
        callAs("GET", "auth/me", null, null, false, 401);
        assertTrue(callAs("GET", userPath, null, auth, false, 403).body().contains("ACCESS_DENIED"));
        assertTrue(callAs("POST", "episodes/1/schedule", "{}", auth, true, 403).body().contains("ACCESS_DENIED"));
        callAs("PATCH", userPath + "/access", "{\"role\":\"EDITOR\",\"enabled\":true}", auth, true, 403);
        String adminPath = "users/" + JSON.readTree(call("GET", "auth/me", null, 200).body()).get("userId").asLong();
        call("DELETE", adminPath, null, 403);
        call("PATCH", adminPath + "/access", "{\"role\":\"READER\",\"enabled\":false}", 403);
        call("PATCH", userPath + "/access", "{\"role\":\"ADMIN\",\"enabled\":true}", 400);
        call("PATCH", userPath + "/access", "{\"role\":\"EDITOR\",\"enabled\":false}", 204);
        callAs("GET", "auth/me", null, auth, false, 401);
        call("PATCH", userPath + "/access", "{\"role\":\"EDITOR\",\"enabled\":true}", 204);
        assertEquals("EDITOR", JSON.readTree(callAs("GET", "auth/me", null, auth, false, 200).body()).get("role").asString());
        callAs("POST", "podcasts", "{}", auth, true, 400);
        callAs("POST", "podcasts", "{}", auth, false, 403);
        call("DELETE", userPath, null, 204);
        userPath = null;
        callAs("GET", "auth/me", null, auth, false, 401);
    }
}
