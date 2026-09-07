package com.demo.upi_offline_mesh.mesh;


import com.demo.upi_offline_mesh.crypto.HybridCryptoService;
import com.demo.upi_offline_mesh.model.MeshPacket;
import com.demo.upi_offline_mesh.model.PaymentInstruction;
import com.demo.upi_offline_mesh.service.BridgeIngestionService;
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

    public DemoService(MeshSimulatorService meshSimulatorService,
                       HybridCryptoService cryptoService,
                       BridgeIngestionService bridgeIngestionService,
                       ObjectMapper objectMapper) {
        this.meshSimulatorService = meshSimulatorService;
        this.cryptoService = cryptoService;
        this.bridgeIngestionService = bridgeIngestionService;
        this.objectMapper = objectMapper;
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
        List<MeshPacket> collected = meshSimulatorService.collectFromBridges();
        List<BridgeIngestionService.IngestResult> results = new ArrayList<>();
        for (MeshPacket packet : collected) {
            results.add(bridgeIngestionService.ingest(packet));
        }
        return results;
    }
}
