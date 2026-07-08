package com.example.WordDocumentsFiller.entities;

import com.example.WordDocumentsFiller.entities.enums.CanadianDocumentStatus;
import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "canadian_vehicles")
public class CanadianVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_name", length = 128)
    private String clientName;

    @Column(name = "vehicle_name", length = 255)
    private String vehicleName;

    @Column(length = 64)
    private String vin;

    @Column(name = "container_no", length = 64)
    private String containerNo;

    @Column(name = "tracking_link", length = 2048)
    private String trackingLink;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eta;

    @Enumerated(EnumType.STRING)
    @Column(name = "talon_status", nullable = false, length = 32)
    private CanadianDocumentStatus talonStatus = CanadianDocumentStatus.MISSING;

    @Column(name = "invoice_number", length = 128)
    private String invoiceNumber;

    private Double amount;

    @Column(name = "payment_date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate paymentDate;

    @Column(name = "payment_order_file_name", length = 255)
    private String paymentOrderFileName;

    @Column(name = "payment_order_storage_path", length = 1024)
    private String paymentOrderStoragePath;

    @Column(name = "payment_order_uploaded_at")
    private LocalDateTime paymentOrderUploadedAt;

    @Column(name = "payment_orders_sent_at")
    private LocalDateTime paymentOrdersSentAt;

    @Column(name = "payment_orders_sent_to", length = 255)
    private String paymentOrdersSentTo;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public Long getId() { return id; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getContainerNo() { return containerNo; }
    public void setContainerNo(String containerNo) { this.containerNo = containerNo; }

    public String getTrackingLink() { return trackingLink; }
    public void setTrackingLink(String trackingLink) { this.trackingLink = trackingLink; }

    public LocalDate getEta() { return eta; }
    public void setEta(LocalDate eta) { this.eta = eta; }

    public CanadianDocumentStatus getTalonStatus() { return talonStatus; }
    public void setTalonStatus(CanadianDocumentStatus talonStatus) { this.talonStatus = talonStatus; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentOrderFileName() { return paymentOrderFileName; }
    public void setPaymentOrderFileName(String paymentOrderFileName) { this.paymentOrderFileName = paymentOrderFileName; }

    public String getPaymentOrderStoragePath() { return paymentOrderStoragePath; }
    public void setPaymentOrderStoragePath(String paymentOrderStoragePath) { this.paymentOrderStoragePath = paymentOrderStoragePath; }

    public LocalDateTime getPaymentOrderUploadedAt() { return paymentOrderUploadedAt; }
    public void setPaymentOrderUploadedAt(LocalDateTime paymentOrderUploadedAt) { this.paymentOrderUploadedAt = paymentOrderUploadedAt; }

    public LocalDateTime getPaymentOrdersSentAt() { return paymentOrdersSentAt; }
    public void setPaymentOrdersSentAt(LocalDateTime paymentOrdersSentAt) { this.paymentOrdersSentAt = paymentOrdersSentAt; }

    public String getPaymentOrdersSentTo() { return paymentOrdersSentTo; }
    public void setPaymentOrdersSentTo(String paymentOrdersSentTo) { this.paymentOrdersSentTo = paymentOrdersSentTo; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
