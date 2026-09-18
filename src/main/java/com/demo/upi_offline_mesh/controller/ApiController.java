package com.demo.upi_offline_mesh.controller;


import com.demo.upi_offline_mesh.Repository.AccountRepository;
import com.demo.upi_offline_mesh.Repository.TransactionRepository;
import com.demo.upi_offline_mesh.mesh.DemoService;
import com.demo.upi_offline_mesh.mesh.MeshSimulatorService;
import com.demo.upi_offline_mesh.model.Account;
import com.demo.upi_offline_mesh.service.BridgeIngestionService;
import com.demo.upi_offline_mesh.service.IdempotencyService;
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
    private final AccountRepository accountRepository;
    private final IdempotencyService idempotencyService;   // NEW

    public ApiController(DemoService demoService,
                         MeshSimulatorService meshSimulatorService,
                         TransactionRepository transactionRepository,
                         AccountRepository accountRepository,
                         IdempotencyService idempotencyService) {   // NEW parameter
        this.demoService = demoService;
        this.meshSimulatorService = meshSimulatorService;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.idempotencyService = idempotencyService;   // NEW
    }

    @GetMapping("/accounts")
    public List<Account> getAccounts() {
        return accountRepository.findAll();
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

    @GetMapping("/mesh/state")
    public Map<String, Object> getMeshState() {
        List<Map<String, Object>> devices = meshSimulatorService.getDevices().stream()
                .map(d -> Map.<String, Object>of(
                        "deviceId", d.getDeviceId(),
                        "hasInternet", d.hasInternet(),
                        "packetCount", d.getInbox().size(),
                        "packetIds", d.getInbox().stream()
                                .map(p -> p.getPacketId().substring(0, 8))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return Map.of(
                "devices", devices,
                "idempotencyCacheSize", idempotencyService.size()
        );
    }

    @PostMapping("/mesh/reset")
    public Map<String, String> resetMesh() {
        demoService.resetAll();
        return Map.of("status", "mesh, cache, transactions and balances reset");
    }

    // request DTO — uses a Java record
    public record InjectRequest(String senderVpa, String receiverVpa, BigDecimal amount) {}
}

