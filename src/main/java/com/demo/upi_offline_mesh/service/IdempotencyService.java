package com.demo.upi_offline_mesh.service;

import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private final ConcurrentHashMap<String, Instant> seenHashes = new ConcurrentHashMap<>();
    /**
     * Returns true if this is the FIRST time we've seen this hash.
     * Returns false if it's a duplicate (reject / skip).
     */
    public boolean markIfFirstSeen(String packetHash) {
        Instant previous = seenHashes.putIfAbsent(packetHash, Instant.now());
        return previous == null;
    }
    public boolean isAlreadySettled(String packetHash) {
        return seenHashes.containsKey(packetHash);
    }
}
