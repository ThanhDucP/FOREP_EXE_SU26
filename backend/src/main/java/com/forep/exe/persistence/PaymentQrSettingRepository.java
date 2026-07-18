package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentQrSettingRepository extends JpaRepository<PaymentQrSettingEntity, UUID> {
    Optional<PaymentQrSettingEntity> findByPaymentMethod(PaymentMethod paymentMethod);
}
