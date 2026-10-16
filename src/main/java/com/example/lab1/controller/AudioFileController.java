package com.example.lab1.controller;

import com.example.lab1.model.AudioFile;
import com.example.lab1.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/audio-files")
public class AudioFileController {
    private final CatalogService service;

    public AudioFileController(CatalogService service) {
        this.service = service;
    }

    public record Request(Long episodeId, String path, String format, Long sizeBytes, Boolean primary) {}

    @GetMapping
    public List<AudioFile> list() {
        return service.listAudioFiles();
    }

    @GetMapping("/{id}")
    public AudioFile get(@PathVariable Long id) {
        return service.getAudioFile(id);
    }

    @PostMapping
    public ResponseEntity<AudioFile> create(@RequestBody Request request) {
        AudioFile created = service.saveAudioFile(null, request.episodeId(), request.path(), request.format(), request.sizeBytes(), request.primary());
        return ResponseEntity.created(URI.create("/api/audio-files/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    public AudioFile update(@PathVariable Long id, @RequestBody Request request) {
        return service.saveAudioFile(id, request.episodeId(), request.path(), request.format(), request.sizeBytes(), request.primary());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteAudioFile(id);
        return ResponseEntity.noContent().build();
    }
}
