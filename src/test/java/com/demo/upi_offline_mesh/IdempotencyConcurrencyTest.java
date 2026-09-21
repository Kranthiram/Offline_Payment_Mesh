package com.demo.upi_offline_mesh;

import com.demo.upi_offline_mesh.crypto.HybridCryptoService;
import com.demo.upi_offline_mesh.model.MeshPacket;
import com.demo.upi_offline_mesh.model.PaymentInstruction;
import com.demo.upi_offline_mesh.service.BridgeIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class IdempotencyConcurrencyTest {

    @Autowired
    private BridgeIngestionService bridgeIngestionService;

    @Autowired
    private HybridCryptoService cryptoService;

    @Autowired
    private ObjectMapper objectMapper;

    private MeshPacket sharedPacket;

    @BeforeEach
    void setUp() throws Exception {
        // build ONE payment packet that all threads will submit simultaneously
        PaymentInstruction instruction = new PaymentInstruction(
                "alice@upimesh", "bob@upimesh", new BigDecimal("50.00"),
                "hashed-pin-placeholder", UUID.randomUUID().toString(),
                System.currentTimeMillis()
        );
        String plaintextJson = objectMapper.writeValueAsString(instruction);
        String ciphertext = cryptoService.encrypt(plaintextJson);

        // MeshPacket now takes hopCount as the 3rd argument
        sharedPacket = new MeshPacket(
                UUID.randomUUID().toString(),   // packetId
                5,                                // ttl
                2,                                // hopCount (simulating packet that already hopped twice)
                System.currentTimeMillis(),       // createdAt
                ciphertext
        );
    }

    @Test
    void exactlyOneThreadSettlesDuplicatePacket() throws InterruptedException {
        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger settledCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        List<BridgeIngestionService.IngestResult> results =
                Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final String bridgeId = "bridge-thread-" + i;
            executor.submit(() -> {
                try {
                    startGate.await();

                    // ingest() now also takes the bridge device id
                    BridgeIngestionService.IngestResult result =
                            bridgeIngestionService.ingest(sharedPacket, bridgeId);

                    results.add(result);
                    if ("SETTLED".equals(result.status)) {
                        settledCount.incrementAndGet();
                    } else if ("DUPLICATE".equals(result.status)) {
                        duplicateCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, settledCount.get(), "Exactly one thread should settle the packet");
        assertEquals(2, duplicateCount.get(), "The other two should be rejected as duplicates");
    }
}
