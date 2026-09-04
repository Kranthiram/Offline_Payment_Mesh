package com.demo.upi_offline_mesh.service;

import com.demo.upi_offline_mesh.Repository.AccountRepository;
import com.demo.upi_offline_mesh.Repository.TransactionRepository;
import com.demo.upi_offline_mesh.model.Account;
import com.demo.upi_offline_mesh.model.PaymentInstruction;
import com.demo.upi_offline_mesh.model.Transaction;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class SettlementService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public SettlementService(AccountRepository accountRepository,
                             TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction settle(PaymentInstruction instruction, String packetHash) {
        Account sender = accountRepository.findById(instruction.getSenderVpa())
                .orElseThrow(() -> new IllegalArgumentException("Unknown sender: " + instruction.getSenderVpa()));
        Account receiver = accountRepository.findById(instruction.getReceiverVpa())
                .orElseThrow(() -> new IllegalArgumentException("Unknown receiver: " + instruction.getReceiverVpa()));

        BigDecimal amount = instruction.getAmount();

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance for " + sender.getVpa());
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        try {
            accountRepository.save(sender);
            accountRepository.save(receiver);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new IllegalStateException("Concurrent update detected, settlement aborted", e);
        }
        Transaction transaction = new Transaction(
                sender.getVpa(), receiver.getVpa(), amount, packetHash);
        return transactionRepository.save(transaction);
    }
}
