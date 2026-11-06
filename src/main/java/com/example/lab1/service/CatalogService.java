package com.example.lab1.service;

import com.example.lab1.model.*;
import com.example.lab1.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class CatalogService {
    private final CatalogWriteLock lock;
    private final UserRepository users;
    private final PodcastRepository podcasts;
    private final EpisodeRepository episodes;
    private final AudioFileRepository audioFiles;
    private final PublicationSlotRepository publicationSlots;

    public CatalogService(CatalogWriteLock lock, UserRepository users, PodcastRepository podcasts, EpisodeRepository episodes,
                          AudioFileRepository audioFiles, PublicationSlotRepository publicationSlots) {
        this.lock = lock;
        this.users = users;
        this.podcasts = podcasts;
        this.episodes = episodes;
        this.audioFiles = audioFiles;
        this.publicationSlots = publicationSlots;
    }

    static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private static <T extends Resource> T require(JdbcRepository<T> repository, Long id, String type) {
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

    public List<User> listUsers() { return users.findAll(); }
    public List<Podcast> listPodcasts() { return podcasts.findAll(); }
    public List<Episode> listEpisodes() { return episodes.findAll(); }
    public List<AudioFile> listAudioFiles() { return audioFiles.findAll(); }
    public List<PublicationSlot> listPublicationSlots() { return publicationSlots.findAll(); }

    public User getUser(Long id) { return require(users, id, "User"); }
    public Podcast getPodcast(Long id) { return require(podcasts, id, "Podcast"); }
    public Episode getEpisode(Long id) { return require(episodes, id, "Episode"); }
    public AudioFile getAudioFile(Long id) { return require(audioFiles, id, "AudioFile"); }
    public PublicationSlot getPublicationSlot(Long id) { return require(publicationSlots, id, "PublicationSlot"); }

    public User saveUser(Long id, String name, String email) {
        lock.acquire();
        User old = id == null ? null : getUser(id);
        name = text(merge(name, old == null ? null : old.name()), "name");
        email = text(merge(email, old == null ? null : old.email()), "email");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw bad("email is invalid");
        return users.save(id, name, email);
    }

    public Podcast savePodcast(Long id, String title, Long authorId, String status) {
        lock.acquire();
        Podcast old = id == null ? null : getPodcast(id);
        title = text(merge(title, old == null ? null : old.title()), "title");
        authorId = merge(authorId, old == null ? null : old.authorId());
        getUser(authorId);
        status = status(merge(status, old == null ? null : old.status()), "status", "DRAFT", "ACTIVE", "ARCHIVED");
        if (old != null && !status.equals("ACTIVE")
                && (episodes.hasPublishedEpisodes(id) || publicationSlots.hasScheduledSlots(id))) {
            throw conflict("Use archive to close a podcast with published episodes or scheduled slots");
        }
        return podcasts.save(id, title, authorId, status);
    }

    public Episode saveEpisode(Long id, Long podcastId, String title, Integer durationSeconds,
                               String publicationState) {
        lock.acquire();
        Episode old = id == null ? null : getEpisode(id);
        podcastId = merge(podcastId, old == null ? null : old.podcastId());
        getPodcast(podcastId);
        title = text(merge(title, old == null ? null : old.title()), "title");
        durationSeconds = merge(durationSeconds, old == null ? null : old.durationSeconds());
        if (durationSeconds == null || durationSeconds <= 0) throw bad("durationSeconds must be positive");
        publicationState = status(merge(publicationState, old == null ? null : old.publicationState()),
                "publicationState", "DRAFT", "PUBLISHED", "UNPUBLISHED");
        boolean wasPublished = old != null && old.publicationState().equals("PUBLISHED");
        if (wasPublished != publicationState.equals("PUBLISHED")) {
            throw conflict("Use publish or withdraw to change the publication state");
        }
        if (old != null && !old.podcastId().equals(podcastId)
                && (wasPublished || publicationSlots.hasSlots(old.podcastId(), id))) {
            throw conflict("Withdraw the episode and remove its slots before moving it");
        }
        return episodes.save(id, podcastId, title, durationSeconds, publicationState);
    }

    public AudioFile saveAudioFile(Long id, Long episodeId, String path, String format,
                                  Long sizeBytes, Boolean primary) {
        lock.acquire();
        AudioFile old = id == null ? null : getAudioFile(id);
        episodeId = merge(episodeId, old == null ? null : old.episodeId());
        getEpisode(episodeId);
        path = text(merge(path, old == null ? null : old.path()), "path");
        format = text(merge(format, old == null ? null : old.format()), "format");
        sizeBytes = merge(sizeBytes, old == null ? null : old.sizeBytes());
        if (sizeBytes == null || sizeBytes <= 0) throw bad("sizeBytes must be positive");
        primary = merge(primary, old == null ? null : old.primary());
        if (primary == null) throw bad("primary is required");
        if (old != null && (!primary || !old.episodeId().equals(episodeId))) requireRemovableAudio(old);
        return audioFiles.save(id, episodeId, path, format, sizeBytes, primary);
    }

    public PublicationSlot savePublicationSlot(Long id, Long podcastId, Long episodeId,
                                               OffsetDateTime scheduledAt, String status) {
        lock.acquire();
        PublicationSlot old = id == null ? null : getPublicationSlot(id);
        podcastId = merge(podcastId, old == null ? null : old.podcastId());
        episodeId = merge(episodeId, old == null ? null : old.episodeId());
        Podcast podcast = getPodcast(podcastId);
        Episode episode = getEpisode(episodeId);
        if (!episode.podcastId().equals(podcastId)) throw bad("Episode must belong to the slot's podcast");
        scheduledAt = merge(scheduledAt, old == null ? null : old.scheduledAt());
        if (scheduledAt == null) throw bad("scheduledAt is required (ISO 8601 with UTC offset)");
        status = status(merge(status, old == null ? null : old.status()), "status",
                "SCHEDULED", "PUBLISHED", "CANCELLED", "WITHDRAWN");
        if (old != null && isReleaseSlot(old.status())) {
            if (status.equals(old.status()) && episodeId.equals(old.episodeId())
                    && podcastId.equals(old.podcastId()) && scheduledAt.isEqual(old.scheduledAt())) return old;
            throw conflict("A published slot records a completed release");
        }
        if (isReleaseSlot(status)) throw conflict("Use publish or withdraw to change the release status");
        if (status.equals("SCHEDULED")) requireSchedulable(podcast, episode);
        return publicationSlots.save(id, podcastId, episodeId, scheduledAt, status);
    }

    private static boolean isReleaseSlot(String status) {
        return status.equals("PUBLISHED") || status.equals("WITHDRAWN");
    }

    static void requireSchedulable(Podcast podcast, Episode episode) {
        if (!podcast.status().equals("ACTIVE")) throw conflict("Podcast must be active");
        if (episode.publicationState().equals("PUBLISHED")) throw conflict("Episode is already published");
    }

    private void requireRemovableAudio(AudioFile file) {
        if (file.primary() && getEpisode(file.episodeId()).publicationState().equals("PUBLISHED")) {
            throw conflict("Switch the primary audio or withdraw the episode first");
        }
    }

    public void deleteUser(Long id) {
        lock.acquire();
        getUser(id);
        users.deleteById(id);
    }

    public void deletePodcast(Long id) {
        lock.acquire();
        getPodcast(id);
        podcasts.deleteById(id);
    }

    public void deleteEpisode(Long id) {
        lock.acquire();
        getEpisode(id);
        episodes.deleteById(id);
    }

    public void deleteAudioFile(Long id) {
        lock.acquire();
        requireRemovableAudio(getAudioFile(id));
        audioFiles.deleteById(id);
    }

    public void deletePublicationSlot(Long id) {
        lock.acquire();
        PublicationSlot slot = getPublicationSlot(id);
        if (slot.status().equals("PUBLISHED")) throw conflict("Withdraw the episode before removing its release slot");
        publicationSlots.deleteById(id);
    }
}
