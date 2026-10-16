package com.example.lab1.controller;

import com.example.lab1.model.Podcast;
import com.example.lab1.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/podcasts")
public class PodcastController {
    private final CatalogService service;

    public PodcastController(CatalogService service) {
        this.service = service;
    }

    public record Request(String title, Long authorId, String status) {}

    @GetMapping
    public List<Podcast> list() {
        return service.listPodcasts();
    }

    @GetMapping("/{id}")
    public Podcast get(@PathVariable Long id) {
        return service.getPodcast(id);
    }

    @PostMapping
    public ResponseEntity<Podcast> create(@RequestBody Request request) {
        Podcast created = service.savePodcast(null, request.title(), request.authorId(), request.status());
        return ResponseEntity.created(URI.create("/api/podcasts/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    public Podcast update(@PathVariable Long id, @RequestBody Request request) {
        return service.savePodcast(id, request.title(), request.authorId(), request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deletePodcast(id);
        return ResponseEntity.noContent().build();
    }
}
