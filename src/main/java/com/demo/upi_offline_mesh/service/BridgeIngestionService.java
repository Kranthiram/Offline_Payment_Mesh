package com.demo.upi_offline_mesh.service;


import com.demo.upi_offline_mesh.crypto.HybridCryptoService;
import com.demo.upi_offline_mesh.model.MeshPacket;
import com.demo.upi_offline_mesh.model.PaymentInstruction;
import com.demo.upi_offline_mesh.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class BridgeIngestionService {

    private final HybridCryptoService cryptoService;
    private final IdempotencyService idempotencyService;
    private final SettlementService settlementService;
    private final ObjectMapper objectMapper;

    public BridgeIngestionService(HybridCryptoService cryptoService,
                                  IdempotencyService idempotencyService,
                                  SettlementService settlementService,
                                  ObjectMapper objectMapper) {
        this.cryptoService = cryptoService;
        this.idempotencyService = idempotencyService;
        this.settlementService = settlementService;
        this.objectMapper = objectMapper;
    }

    public IngestResult ingest(MeshPacket packet) {
        try {
            String packetHash = cryptoService.hashCiphertext(packet.getCiphertext());

            // idempotency check FIRST — avoid decrypting duplicates unnecessarily
            boolean isFirstTime = idempotencyService.markIfFirstSeen(packetHash);
            if (!isFirstTime) {
                return IngestResult.duplicate(packetHash);
            }

            String plaintextJson = cryptoService.decrypt(packet.getCiphertext());
            PaymentInstruction instruction = objectMapper.readValue(plaintextJson, PaymentInstruction.class);

            Transaction transaction = settlementService.settle(instruction, packetHash);
            return IngestResult.success(transaction);

        } catch (Exception e) {
            return IngestResult.failure(e.getMessage());
        }
    }

    // simple inner result wrapper
    public static class IngestResult {
        public final String status;
        public final String detail;
        public final Transaction transaction;

        private IngestResult(String status, String detail, Transaction transaction) {
            this.status = status;
            this.detail = detail;
            this.transaction = transaction;
        }

        public static IngestResult success(Transaction t) {
            return new IngestResult("SETTLED", null, t);
        }

        public static IngestResult duplicate(String hash) {
            return new IngestResult("DUPLICATE", "Already processed: " + hash, null);
        }

        public static IngestResult failure(String reason) {
            return new IngestResult("FAILED", reason, null);
        }
    }
}