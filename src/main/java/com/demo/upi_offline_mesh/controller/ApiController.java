package com.demo.upi_offline_mesh.controller;


import com.demo.upi_offline_mesh.Repository.TransactionRepository;
import com.demo.upi_offline_mesh.mesh.DemoService;
import com.demo.upi_offline_mesh.mesh.MeshSimulatorService;
import com.demo.upi_offline_mesh.service.BridgeIngestionService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final DemoService demoService;
    private final MeshSimulatorService meshSimulatorService;
    private final TransactionRepository transactionRepository;
    public ApiController(DemoService demoService,
                         MeshSimulatorService meshSimulatorService,
                         TransactionRepository transactionRepository) {
        this.demoService = demoService;
        this.meshSimulatorService = meshSimulatorService;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping("/mesh/setup")
    public Map<String, String> setupMesh() {
        demoService.setupMesh();
        return Map.of("status", "mesh initialized with 5 devices");
    }
    @PostMapping("/mesh/inject")
    public Map<String, String> injectPayment(@RequestBody InjectRequest request) throws Exception {
        demoService.injectPayment(request.senderVpa(), request.receiverVpa(), request.amount());
        return Map.of("status", "payment injected into phone-A");
    }
    @PostMapping("/mesh/propagate")
    public Map<String, String> propagate() {
        demoService.propagate();
        return Map.of("status", "gossip rounds complete");
    }
    @PostMapping("/mesh/sync")
    public List<BridgeIngestionService.IngestResult> syncBridges() {
        return demoService.syncBridges();
    }
    @GetMapping("/mesh/devices")
    public List<Map<String, Object>> getDevices() {
        return meshSimulatorService.getDevices().stream()
                .map(d -> Map.<String, Object>of(
                        "deviceId", d.getDeviceId(),
                        "hasInternet", d.hasInternet(),
                        "inboxSize", d.getInbox().size()
                ))
                .collect(Collectors.toList());
    }
    @GetMapping("/transactions")
    public List<?> getTransactions() {
        return transactionRepository.findTop20ByOrderBySettledAtDesc();
    }

    // request DTO — uses a Java record
    public record InjectRequest(String senderVpa, String receiverVpa, BigDecimal amount) {}
}
