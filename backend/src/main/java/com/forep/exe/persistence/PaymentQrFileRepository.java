package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentQrFileRepository extends JpaRepository<PaymentQrFileEntity, UUID> {
}
