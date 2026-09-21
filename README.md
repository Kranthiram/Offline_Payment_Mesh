# UPI Offline Mesh — Peer-to-Peer Payment Settlement System

A Spring Boot backend that simulates offline UPI payments propagating through a device mesh network via a gossip protocol, settling once a packet reaches an internet-connected bridge device.

## The Idea

Imagine you're in a basement with no internet, and you owe a friend ₹500. Your phone can't reach a UPI server — but it can talk to nearby phones over Bluetooth/WiFi-Direct. This project simulates that: a payment packet, encrypted end-to-end, hops from phone to phone until one of them finds an internet connection and uploads it to the backend for settlement.

## Tech Stack

Java 17 · Spring Boot 3.3 · Spring Data JPA · H2 (in-memory) · JUnit 5 · Thymeleaf · HTML/CSS/JS

## Key Engineering Pieces

- **Hybrid encryption** — RSA-2048 (OAEP padding) wraps a fresh AES-256-GCM key per packet, so payment payloads stay unreadable to every intermediate device in the mesh. Only the backend, holding the private key, can decrypt them.

- **Idempotent settlement** — duplicate packets (common in gossip networks, where the same packet can reach the backend via multiple paths) are rejected using `ConcurrentHashMap.putIfAbsent()` for an atomic, thread-safe dedupe check, backed by a database-level unique constraint as a second layer of defense.

- **Optimistic locking** — account balances use JPA's `@Version` field to detect and reject concurrent conflicting updates without holding database locks.

- **Gossip propagation** — packets fan out to 1-2 random neighbor devices per round, with a TTL that decrements on each hop to prevent infinite circulation, and a separate hop counter for analytics.

## Running It

./mvnw spring-boot:run


Then open `http://localhost:8080`. The dashboard walks through the full flow: initialize a 5-device mesh → inject an encrypted payment → run gossip rounds → sync the bridge device to settle it on the backend.

## Testing

`IdempotencyConcurrencyTest` uses `CountDownLatch` to force three threads to submit an identical packet at the same instant, from three different simulated bridge devices — the kind of race condition that can genuinely happen when a packet reaches the backend via multiple mesh paths. It asserts exactly one settlement and two rejected duplicates, verifying the atomic dedupe logic holds under real concurrent access.

## Architecture

model/ — JPA entities (Account, Transaction) and plain DTOs (MeshPacket, PaymentInstruction)
Repository/ — AccountRepository, TransactionRepository
crypto/ — RSA + AES-GCM hybrid encryption
service/ — Idempotency, settlement, and ingestion orchestration
mesh/ — Virtual device simulation and gossip propagation
controller/ — REST API + dashboard
