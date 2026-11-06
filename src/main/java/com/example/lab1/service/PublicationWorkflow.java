package com.example.lab1.service;

import com.example.lab1.model.*;
import com.example.lab1.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;

import static com.example.lab1.service.CatalogService.bad;
import static com.example.lab1.service.CatalogService.conflict;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class PublicationWorkflow {
    private final CatalogWriteLock lock;
    private final CatalogService catalog;
    private final PodcastRepository podcasts;
    private final EpisodeRepository episodes;
    private final AudioFileRepository audioFiles;
    private final PublicationSlotRepository slots;

    public PublicationWorkflow(CatalogWriteLock lock, CatalogService catalog, PodcastRepository podcasts,
                               EpisodeRepository episodes, AudioFileRepository audioFiles,
                               PublicationSlotRepository slots) {
        this.lock = lock;
        this.catalog = catalog;
        this.podcasts = podcasts;
        this.episodes = episodes;
        this.audioFiles = audioFiles;
        this.slots = slots;
    }

    public PublicationSlot schedule(Long episodeId, OffsetDateTime scheduledAt) {
        lock.acquire();
        Episode episode = catalog.getEpisode(episodeId);
        if (scheduledAt == null || !scheduledAt.toInstant().isAfter(Instant.now())) {
            throw bad("scheduledAt must be in the future");
        }
        requireReadyToPublish(episode);
        return slots.save(null, episode.podcastId(), episode.id(), scheduledAt, "SCHEDULED");
    }

    public record Publication(Episode episode, PublicationSlot slot) {}

    public Publication publish(Long slotId) {
        lock.acquire();
        PublicationSlot slot = catalog.getPublicationSlot(slotId);
        if (!slot.status().equals("SCHEDULED")) throw conflict("Only a scheduled slot can be published");
        if (slot.scheduledAt().toInstant().isAfter(Instant.now())) throw conflict("Publication time has not arrived");
        Episode episode = catalog.getEpisode(slot.episodeId());
        requireReadyToPublish(episode);
        return new Publication(episodes.changeState(episode, "PUBLISHED"), slots.changeStatus(slot, "PUBLISHED"));
    }

    public Episode withdraw(Long episodeId) {
        lock.acquire();
        Episode episode = catalog.getEpisode(episodeId);
        if (!episode.publicationState().equals("PUBLISHED")) throw conflict("Episode is not published");
        if (slots.withdrawRelease(episodeId) != 1) throw conflict("Episode has no current release slot");
        return episodes.changeState(episode, "UNPUBLISHED");
    }

    public AudioFile switchPrimary(Long episodeId, Long fileId) {
        lock.acquire();
        Episode episode = catalog.getEpisode(episodeId);
        AudioFile replacement = catalog.getAudioFile(fileId);
        if (!replacement.episodeId().equals(episode.id())) throw conflict("Audio file belongs to another episode");
        AudioFile current = audioFiles.findPrimary(episodeId)
                .orElseThrow(() -> conflict("Episode has no primary audio to replace"));
        if (current.id().equals(fileId)) return current;
        audioFiles.changePrimary(current, false);
        return audioFiles.changePrimary(replacement, true);
    }

    public record ArchiveResult(Podcast podcast, int withdrawnEpisodes, int cancelledSlots) {}

    public ArchiveResult archive(Long podcastId) {
        lock.acquire();
        Podcast podcast = catalog.getPodcast(podcastId);
        if (!podcast.status().equals("ACTIVE")) throw conflict("Only an active podcast can be archived");
        int cancelled = slots.cancelForPodcast(podcastId);
        int withdrawn = episodes.withdrawForPodcast(podcastId);
        slots.withdrawForPodcast(podcastId);
        return new ArchiveResult(podcasts.save(podcastId, podcast.title(), podcast.authorId(), "ARCHIVED"),
                withdrawn, cancelled);
    }

    private void requireReadyToPublish(Episode episode) {
        CatalogService.requireSchedulable(catalog.getPodcast(episode.podcastId()), episode);
        if (audioFiles.findPrimary(episode.id()).isEmpty()) throw conflict("Episode has no primary audio");
    }
}
