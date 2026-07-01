package com.forep.exe.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class WorkspaceEntity extends BaseEntity {
    @Column(nullable = false)
    private String name;
    private String logo;
    @Column(columnDefinition = "text")
    private String address;
    @Column(unique = true)
    private String shortCode;
    @Column(nullable = false)
    private int nextEmployeeNumber = 1;
    private UUID ownerId;
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public int getNextEmployeeNumber() {
        return nextEmployeeNumber;
    }

    public void setNextEmployeeNumber(int nextEmployeeNumber) {
        this.nextEmployeeNumber = nextEmployeeNumber;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
