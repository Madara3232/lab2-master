package com.example.lab1.controller;

import com.example.lab1.model.Episode;
import com.example.lab1.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/episodes")
public class EpisodeController {
    private final CatalogService service;

    public EpisodeController(CatalogService service) {
        this.service = service;
    }

    public record Request(Long podcastId, String title, Integer durationSeconds, String publicationState) {}

    @GetMapping
    public List<Episode> list() {
        return service.listEpisodes();
    }

    @GetMapping("/{id}")
    public Episode get(@PathVariable Long id) {
        return service.getEpisode(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public ResponseEntity<Episode> create(@RequestBody Request request) {
        Episode created = service.saveEpisode(null, request.podcastId(), request.title(), request.durationSeconds(), request.publicationState());
        return ResponseEntity.created(URI.create("/api/episodes/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public Episode update(@PathVariable Long id, @RequestBody Request request) {
        return service.saveEpisode(id, request.podcastId(), request.title(), request.durationSeconds(), request.publicationState());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteEpisode(id);
        return ResponseEntity.noContent().build();
    }
}
