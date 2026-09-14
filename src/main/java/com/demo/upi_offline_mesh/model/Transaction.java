package com.demo.upi_offline_mesh.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = "packet_hash")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String senderVpa;
    private String receiverVpa;
    private BigDecimal amount;

    @Column(name = "packet_hash", unique = true)
    private String packetHash;

    private String bridgeNodeId;   // NEW — which bridge device uploaded this packet
    private int hopCount;          // NEW — how many hops the packet took before settling

    private Instant settledAt;

    public Transaction() {}

    public Transaction(String senderVpa, String receiverVpa, BigDecimal amount,
                       String packetHash, String bridgeNodeId, int hopCount) {
        this.senderVpa = senderVpa;
        this.receiverVpa = receiverVpa;
        this.amount = amount;
        this.packetHash = packetHash;
        this.bridgeNodeId = bridgeNodeId;
        this.hopCount = hopCount;
        this.settledAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getSenderVpa() { return senderVpa; }
    public String getReceiverVpa() { return receiverVpa; }
    public BigDecimal getAmount() { return amount; }
    public String getPacketHash() { return packetHash; }
    public String getBridgeNodeId() { return bridgeNodeId; }
    public int getHopCount() { return hopCount; }
    public Instant getSettledAt() { return settledAt; }
}