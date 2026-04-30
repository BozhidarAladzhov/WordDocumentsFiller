package com.example.WordDocumentsFiller.entities;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "fleet_maintenance_records")
public class FleetMaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fleet_vehicle_id", nullable = false)
    private FleetVehicle vehicle;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "mileage_km")
    private Integer mileageKm;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "vendor_name", length = 255)
    private String vendorName;

    @Column(columnDefinition = "TEXT")
    private String note;

    public Long getId() { return id; }
    public FleetVehicle getVehicle() { return vehicle; }
    public void setVehicle(FleetVehicle vehicle) { this.vehicle = vehicle; }
    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }
    public Integer getMileageKm() { return mileageKm; }
    public void setMileageKm(Integer mileageKm) { this.mileageKm = mileageKm; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
