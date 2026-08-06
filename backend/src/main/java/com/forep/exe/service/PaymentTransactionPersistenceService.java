package com.forep.exe.service;

import com.forep.exe.domain.Enums.PaymentTransactionStatus;
import com.forep.exe.persistence.PaymentTransactionEntity;
import com.forep.exe.persistence.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class PaymentTransactionPersistenceService {
    private final PaymentTransactionRepository payments;

    public PaymentTransactionPersistenceService(PaymentTransactionRepository payments) {
        this.payments = payments;
    }

    @Transactional
    public PaymentTransactionEntity persistPending(PaymentTransactionEntity payment) {
        return payments.saveAndFlush(payment);
    }

    @Transactional
    public void markProviderFailure(PaymentTransactionEntity payment, String reason) {
        PaymentTransactionEntity current = payments.findByIdForUpdate(payment.getId()).orElse(payment);
        if (current.getStatus() == PaymentTransactionStatus.PAID || current.getStatus() == PaymentTransactionStatus.SUCCESS) {
            return;
        }
        current.setStatus(PaymentTransactionStatus.FAILED);
        current.setFailureReason(reason);
        current.setUpdatedAt(OffsetDateTime.now());
        payments.save(current);
    }
}
