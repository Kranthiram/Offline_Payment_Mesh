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

    private Instant settledAt;

    public Transaction() {};

    public Transaction(String senderVpa, String receiverVpa, BigDecimal amount, String packetHash) {
        this.senderVpa = senderVpa;
        this.receiverVpa = receiverVpa;
        this.amount = amount;
        this.packetHash = packetHash;
    }

    public Long getId() {
        return id;
    }

    public String getSenderVpa() {
        return senderVpa;
    }

    public String getReceiverVpa() {
        return receiverVpa;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPacketHash() {
        return packetHash;
    }

    public Instant getSettledAt() {
        return settledAt;
    }
}
