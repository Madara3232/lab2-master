package com.example.lab1.repository;

import com.example.lab1.model.Resource;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Storage only: business validation and cascades belong to CatalogService. */
public abstract class InMemoryRepository<T extends Resource> {
    private final Map<Long, T> data = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public long nextId() { return sequence.incrementAndGet(); }
    public synchronized List<T> findAll() { return List.copyOf(data.values()); }
    public synchronized Optional<T> findById(Long id) { return Optional.ofNullable(data.get(id)); }
    public synchronized T save(T item) { data.put(item.id(), item); return item; }
    public synchronized void deleteById(Long id) { data.remove(id); }
}
