package com.example.lab1.controller;

import com.example.lab1.model.AudioFile;
import com.example.lab1.model.Episode;
import com.example.lab1.model.PublicationSlot;
import com.example.lab1.service.PublicationWorkflow;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;

@RestController
@PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
@RequestMapping("/api")
public class PublicationWorkflowController {
    private final PublicationWorkflow workflow;

    public PublicationWorkflowController(PublicationWorkflow workflow) {
        this.workflow = workflow;
    }

    public record ScheduleRequest(OffsetDateTime scheduledAt) {}

    @PostMapping("/episodes/{id}/schedule")
    public ResponseEntity<PublicationSlot> schedule(@PathVariable Long id, @RequestBody ScheduleRequest request) {
        PublicationSlot slot = workflow.schedule(id, request.scheduledAt());
        return ResponseEntity.created(URI.create("/api/publication-slots/" + slot.id())).body(slot);
    }

    @PostMapping("/publication-slots/{id}/publish")
    public PublicationWorkflow.Publication publish(@PathVariable Long id) {
        return workflow.publish(id);
    }

    @PostMapping("/episodes/{id}/withdraw")
    public Episode withdraw(@PathVariable Long id) {
        return workflow.withdraw(id);
    }

    public record PrimaryAudioRequest(Long audioFileId) {}

    @PostMapping("/episodes/{id}/switch-primary")
    public AudioFile switchPrimary(@PathVariable Long id, @RequestBody PrimaryAudioRequest request) {
        return workflow.switchPrimary(id, request.audioFileId());
    }

    @PostMapping("/podcasts/{id}/archive")
    public PublicationWorkflow.ArchiveResult archive(@PathVariable Long id) {
        return workflow.archive(id);
    }
}
