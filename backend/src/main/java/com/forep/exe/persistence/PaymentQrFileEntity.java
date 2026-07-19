package com.forep.exe.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_qr_files")
public class PaymentQrFileEntity extends BaseEntity {
    @Column(nullable = false)
    private String fileName;
    @Column(nullable = false)
    private String contentType;
    @Column(nullable = false)
    private long fileSize;
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] content;
    private UUID uploadedBy;
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }
    public UUID getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UUID uploadedBy) { this.uploadedBy = uploadedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
