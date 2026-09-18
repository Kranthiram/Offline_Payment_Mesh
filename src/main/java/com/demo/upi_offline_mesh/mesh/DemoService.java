package com.demo.upi_offline_mesh.mesh;


import com.demo.upi_offline_mesh.Repository.AccountRepository;
import com.demo.upi_offline_mesh.Repository.TransactionRepository;
import com.demo.upi_offline_mesh.crypto.HybridCryptoService;
import com.demo.upi_offline_mesh.model.MeshPacket;
import com.demo.upi_offline_mesh.model.PaymentInstruction;
import com.demo.upi_offline_mesh.service.BridgeIngestionService;
import com.demo.upi_offline_mesh.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DemoService {

    private static final int INITIAL_TTL = 5;
    private static final int GOSSIP_ROUNDS = 4;

    private final MeshSimulatorService meshSimulatorService;
    private final HybridCryptoService cryptoService;
    private final BridgeIngestionService bridgeIngestionService;
    private final ObjectMapper objectMapper;
    private final IdempotencyService idempotencyService;      // NEW
    private final AccountRepository accountRepository;         // NEW
    private final TransactionRepository transactionRepository; // NEW

    public DemoService(MeshSimulatorService meshSimulatorService,
                       HybridCryptoService cryptoService,
                       BridgeIngestionService bridgeIngestionService,
                       ObjectMapper objectMapper,
                       IdempotencyService idempotencyService,       // NEW
                       AccountRepository accountRepository,          // NEW
                       TransactionRepository transactionRepository) { // NEW
        this.meshSimulatorService = meshSimulatorService;
        this.cryptoService = cryptoService;
        this.bridgeIngestionService = bridgeIngestionService;
        this.objectMapper = objectMapper;
        this.idempotencyService = idempotencyService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    // Sets up a fixed mesh of 5 virtual phones — 1 of them has internet (bridge).
    public void setupMesh() {
        meshSimulatorService.getDevices().clear();
        meshSimulatorService.registerDevice(new VirtualDevice("phone-A", false));
        meshSimulatorService.registerDevice(new VirtualDevice("phone-B", false));
        meshSimulatorService.registerDevice(new VirtualDevice("phone-C", false));
        meshSimulatorService.registerDevice(new VirtualDevice("phone-D", false));
        meshSimulatorService.registerDevice(new VirtualDevice("bridge-E", true));
    }

    // Creates one payment, encrypts it, and injects it into the mesh from phone-A.
    public void injectPayment(String senderVpa, String receiverVpa, BigDecimal amount) throws Exception {
        PaymentInstruction instruction = new PaymentInstruction(
                senderVpa, receiverVpa, amount,
                "hashed-pin-placeholder",
                UUID.randomUUID().toString(),
                System.currentTimeMillis()
        );

        String plaintextJson = objectMapper.writeValueAsString(instruction);
        String ciphertext = cryptoService.encrypt(plaintextJson);

        MeshPacket packet = new MeshPacket(
                UUID.randomUUID().toString(),
                INITIAL_TTL,
                0,
                System.currentTimeMillis(),
                ciphertext
        );

        // inject into the first device — simulating the sender's own phone
        meshSimulatorService.getDevices().get(0).receive(packet);
    }

    // Runs several gossip rounds so the packet propagates through the mesh.
    public void propagate() {
        for (int i = 0; i < GOSSIP_ROUNDS; i++) {
            meshSimulatorService.runGossipRound();
        }
    }

    // Collects packets from bridge devices and pushes them to the backend for settlement.
    public List<BridgeIngestionService.IngestResult> syncBridges() {
        List<MeshSimulatorService.BridgeCollection> collected = meshSimulatorService.collectFromBridges();
        List<BridgeIngestionService.IngestResult> results = new ArrayList<>();
        for (MeshSimulatorService.BridgeCollection item : collected) {
            results.add(bridgeIngestionService.ingest(item.packet(), item.bridgeDeviceId()));
        }
        return results;
    }

    public void resetAll() {
        // 1. clear mesh devices and idempotency cache
        meshSimulatorService.resetDevices();
        idempotencyService.clear();

        // 2. delete all transactions
        transactionRepository.deleteAll();

        // 3. reset account balances back to their starting amounts
        accountRepository.findById("alice@upimesh").ifPresent(acc -> {
            acc.setBalance(new BigDecimal("5000.00"));
            accountRepository.save(acc);
        });
        accountRepository.findById("bob@upimesh").ifPresent(acc -> {
            acc.setBalance(new BigDecimal("3000.00"));
            accountRepository.save(acc);
        });

        // 4. re-create the 5 mesh devices
        setupMesh();
    }
}
