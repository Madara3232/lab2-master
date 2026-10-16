package com.example.lab1.controller;

import com.example.lab1.model.PublicationSlot;
import com.example.lab1.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/publication-slots")
public class PublicationSlotController {
    private final CatalogService service;

    public PublicationSlotController(CatalogService service) {
        this.service = service;
    }

    public record Request(Long podcastId, Long episodeId, java.time.OffsetDateTime scheduledAt, String status) {}

    @GetMapping
    public List<PublicationSlot> list() {
        return service.listPublicationSlots();
    }

    @GetMapping("/{id}")
    public PublicationSlot get(@PathVariable Long id) {
        return service.getPublicationSlot(id);
    }

    @PostMapping
    public ResponseEntity<PublicationSlot> create(@RequestBody Request request) {
        PublicationSlot created = service.savePublicationSlot(null, request.podcastId(), request.episodeId(), request.scheduledAt(), request.status());
        return ResponseEntity.created(URI.create("/api/publication-slots/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    public PublicationSlot update(@PathVariable Long id, @RequestBody Request request) {
        return service.savePublicationSlot(id, request.podcastId(), request.episodeId(), request.scheduledAt(), request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deletePublicationSlot(id);
        return ResponseEntity.noContent().build();
    }
}
