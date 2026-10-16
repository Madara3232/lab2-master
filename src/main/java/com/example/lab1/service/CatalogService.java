package com.example.lab1.service;

import com.example.lab1.model.*;
import com.example.lab1.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * Owns validation, partial updates, referential integrity and cascade deletion.
 * The shared monitor makes each operation atomic across all in-memory repositories.
 */
@Service
public class CatalogService {
    private final UserRepository users;
    private final PodcastRepository podcasts;
    private final EpisodeRepository episodes;
    private final AudioFileRepository audioFiles;
    private final PublicationSlotRepository publicationSlots;

    public CatalogService(UserRepository users, PodcastRepository podcasts, EpisodeRepository episodes,
                          AudioFileRepository audioFiles, PublicationSlotRepository publicationSlots) {
        this.users = users;
        this.podcasts = podcasts;
        this.episodes = episodes;
        this.audioFiles = audioFiles;
        this.publicationSlots = publicationSlots;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private static <T extends Resource> T require(InMemoryRepository<T> repository, Long id, String type) {
        if (id == null || id <= 0) throw bad(type + " id must be positive");
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, type + " " + id + " not found"));
    }

    private static <T> T merge(T value, T previous) {
        return value == null ? previous : value;
    }

    private static String text(String value, String field) {
        if (value == null || value.isBlank()) throw bad(field + " is required and must not be blank");
        return value.trim();
    }

    private static String status(String value, String field, String... allowed) {
        value = text(value, field);
        if (!Set.of(allowed).contains(value)) throw bad(field + " must be one of " + String.join(", ", allowed));
        return value;
    }

    public synchronized List<User> listUsers() { return users.findAll(); }
    public synchronized List<Podcast> listPodcasts() { return podcasts.findAll(); }
    public synchronized List<Episode> listEpisodes() { return episodes.findAll(); }
    public synchronized List<AudioFile> listAudioFiles() { return audioFiles.findAll(); }
    public synchronized List<PublicationSlot> listPublicationSlots() { return publicationSlots.findAll(); }

    public synchronized User getUser(Long id) { return require(users, id, "User"); }
    public synchronized Podcast getPodcast(Long id) { return require(podcasts, id, "Podcast"); }
    public synchronized Episode getEpisode(Long id) { return require(episodes, id, "Episode"); }
    public synchronized AudioFile getAudioFile(Long id) { return require(audioFiles, id, "AudioFile"); }
    public synchronized PublicationSlot getPublicationSlot(Long id) { return require(publicationSlots, id, "PublicationSlot"); }

    public synchronized User saveUser(Long id, String name, String email) {
        User old = id == null ? null : getUser(id);
        name = text(merge(name, old == null ? null : old.name()), "name");
        email = text(merge(email, old == null ? null : old.email()), "email");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw bad("email is invalid");
        for (User user : users.findAll()) {
            if (!user.id().equals(id) && user.email().equalsIgnoreCase(email)) throw conflict("email already exists");
        }
        Instant now = Instant.now();
        return users.save(new User(old == null ? users.nextId() : id, name, email,
                old == null ? now : old.createdAt(), now));
    }

    public synchronized Podcast savePodcast(Long id, String title, Long authorId, String status) {
        Podcast old = id == null ? null : getPodcast(id);
        title = text(merge(title, old == null ? null : old.title()), "title");
        authorId = merge(authorId, old == null ? null : old.authorId());
        getUser(authorId);
        status = status(merge(status, old == null ? null : old.status()), "status", "DRAFT", "ACTIVE", "ARCHIVED");
        Instant now = Instant.now();
        return podcasts.save(new Podcast(old == null ? podcasts.nextId() : id, title, authorId, status,
                old == null ? now : old.createdAt(), now));
    }

    public synchronized Episode saveEpisode(Long id, Long podcastId, String title, Integer durationSeconds,
                                            String publicationState) {
        Episode old = id == null ? null : getEpisode(id);
        podcastId = merge(podcastId, old == null ? null : old.podcastId());
        getPodcast(podcastId);
        title = text(merge(title, old == null ? null : old.title()), "title");
        durationSeconds = merge(durationSeconds, old == null ? null : old.durationSeconds());
        if (durationSeconds == null || durationSeconds <= 0) throw bad("durationSeconds must be positive");
        publicationState = status(merge(publicationState, old == null ? null : old.publicationState()),
                "publicationState", "DRAFT", "PUBLISHED", "UNPUBLISHED");
        if (old != null && !old.podcastId().equals(podcastId)) {
            for (PublicationSlot slot : publicationSlots.findAll()) {
                if (slot.episodeId().equals(id)) throw conflict("Delete publication slots before moving this episode");
            }
        }
        Instant now = Instant.now();
        return episodes.save(new Episode(old == null ? episodes.nextId() : id, podcastId, title, durationSeconds,
                publicationState, old == null ? now : old.createdAt(), now));
    }

    public synchronized AudioFile saveAudioFile(Long id, Long episodeId, String path, String format,
                                                Long sizeBytes, Boolean primary) {
        AudioFile old = id == null ? null : getAudioFile(id);
        episodeId = merge(episodeId, old == null ? null : old.episodeId());
        getEpisode(episodeId);
        path = text(merge(path, old == null ? null : old.path()), "path");
        format = text(merge(format, old == null ? null : old.format()), "format");
        sizeBytes = merge(sizeBytes, old == null ? null : old.sizeBytes());
        if (sizeBytes == null || sizeBytes <= 0) throw bad("sizeBytes must be positive");
        primary = merge(primary, old == null ? null : old.primary());
        if (primary == null) throw bad("primary is required");
        if (primary) {
            for (AudioFile file : audioFiles.findAll()) {
                if (!file.id().equals(id) && file.episodeId().equals(episodeId) && file.primary()) {
                    throw conflict("Episode already has a primary audio file");
                }
            }
        }
        Instant now = Instant.now();
        return audioFiles.save(new AudioFile(old == null ? audioFiles.nextId() : id, episodeId, path, format,
                sizeBytes, primary, old == null ? now : old.createdAt(), now));
    }

    public synchronized PublicationSlot savePublicationSlot(Long id, Long podcastId, Long episodeId,
                                                            OffsetDateTime scheduledAt, String status) {
        PublicationSlot old = id == null ? null : getPublicationSlot(id);
        podcastId = merge(podcastId, old == null ? null : old.podcastId());
        episodeId = merge(episodeId, old == null ? null : old.episodeId());
        getPodcast(podcastId);
        Episode episode = getEpisode(episodeId);
        if (!episode.podcastId().equals(podcastId)) throw bad("Episode must belong to the slot's podcast");
        scheduledAt = merge(scheduledAt, old == null ? null : old.scheduledAt());
        if (scheduledAt == null) throw bad("scheduledAt is required (ISO 8601 with UTC offset)");
        status = status(merge(status, old == null ? null : old.status()), "status",
                "SCHEDULED", "PUBLISHED", "CANCELLED");
        Instant now = Instant.now();
        return publicationSlots.save(new PublicationSlot(old == null ? publicationSlots.nextId() : id,
                podcastId, episodeId, scheduledAt, status, old == null ? now : old.createdAt(), now));
    }

    public synchronized void deleteUser(Long id) {
        getUser(id);
        for (Podcast podcast : podcasts.findAll()) {
            if (podcast.authorId().equals(id)) deletePodcast(podcast.id());
        }
        users.deleteById(id);
    }

    public synchronized void deletePodcast(Long id) {
        getPodcast(id);
        for (Episode episode : episodes.findAll()) {
            if (episode.podcastId().equals(id)) deleteEpisode(episode.id());
        }
        for (PublicationSlot slot : publicationSlots.findAll()) {
            if (slot.podcastId().equals(id)) publicationSlots.deleteById(slot.id());
        }
        podcasts.deleteById(id);
    }

    public synchronized void deleteEpisode(Long id) {
        getEpisode(id);
        for (AudioFile file : audioFiles.findAll()) {
            if (file.episodeId().equals(id)) audioFiles.deleteById(file.id());
        }
        for (PublicationSlot slot : publicationSlots.findAll()) {
            if (slot.episodeId().equals(id)) publicationSlots.deleteById(slot.id());
        }
        episodes.deleteById(id);
    }

    public synchronized void deleteAudioFile(Long id) {
        getAudioFile(id);
        audioFiles.deleteById(id);
    }

    public synchronized void deletePublicationSlot(Long id) {
        getPublicationSlot(id);
        publicationSlots.deleteById(id);
    }
}
