package com.forep.exe.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlanEntity, UUID> {
    Optional<SubscriptionPlanEntity> findByNameIgnoreCase(String name);
    List<SubscriptionPlanEntity> findAllByOrderByCreatedAtDesc();
}
