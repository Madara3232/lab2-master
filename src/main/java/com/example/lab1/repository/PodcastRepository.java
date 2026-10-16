package com.example.lab1.repository;

import com.example.lab1.model.Podcast;
import org.springframework.stereotype.Repository;

@Repository
public class PodcastRepository extends InMemoryRepository<Podcast> {
}
