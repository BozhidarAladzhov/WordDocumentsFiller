package com.example.WordDocumentsFiller.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "canadian_vehicle_payment_orders")
public class CanadianVehiclePaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "canadian_vehicle_id", nullable = false)
    private CanadianVehicle canadianVehicle;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "storage_relative_path", nullable = false, length = 1024)
    private String storageRelativePath;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    public Long getId() { return id; }

    public CanadianVehicle getCanadianVehicle() { return canadianVehicle; }
    public void setCanadianVehicle(CanadianVehicle canadianVehicle) { this.canadianVehicle = canadianVehicle; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getStoredFileName() { return storedFileName; }
    public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getStorageRelativePath() { return storageRelativePath; }
    public void setStorageRelativePath(String storageRelativePath) { this.storageRelativePath = storageRelativePath; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
