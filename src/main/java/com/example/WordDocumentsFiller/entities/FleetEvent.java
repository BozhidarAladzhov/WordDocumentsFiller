package com.example.WordDocumentsFiller.entities;

import com.example.WordDocumentsFiller.entities.enums.FleetEventStatus;
import com.example.WordDocumentsFiller.entities.enums.FleetEventType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fleet_events")
public class FleetEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fleet_vehicle_id", nullable = false)
    private FleetVehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FleetEventType type = FleetEventType.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FleetEventStatus status = FleetEventStatus.OPEN;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 255)
    private String vendor;

    @Column(name = "contact_phone", length = 64)
    private String contactPhone;

    @Column(length = 255)
    private String location;

    @Column(name = "scheduled_on")
    private LocalDate scheduledOn;

    @Column(name = "due_on")
    private LocalDate dueOn;

    @Column(name = "completed_on")
    private LocalDate completedOn;

    @Column(name = "notify_days_before")
    private Integer notifyDaysBefore = 30;

    @Column(name = "notify_responsible", nullable = false)
    private boolean notifyResponsible = true;

    @Column(name = "notify_subcontractor", nullable = false)
    private boolean notifySubcontractor;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public Long getId() { return id; }
    public FleetVehicle getVehicle() { return vehicle; }
    public void setVehicle(FleetVehicle vehicle) { this.vehicle = vehicle; }
    public FleetEventType getType() { return type; }
    public void setType(FleetEventType type) { this.type = type; }
    public FleetEventStatus getStatus() { return status; }
    public void setStatus(FleetEventStatus status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDate getScheduledOn() { return scheduledOn; }
    public void setScheduledOn(LocalDate scheduledOn) { this.scheduledOn = scheduledOn; }
    public LocalDate getDueOn() { return dueOn; }
    public void setDueOn(LocalDate dueOn) { this.dueOn = dueOn; }
    public LocalDate getCompletedOn() { return completedOn; }
    public void setCompletedOn(LocalDate completedOn) { this.completedOn = completedOn; }
    public Integer getNotifyDaysBefore() { return notifyDaysBefore; }
    public void setNotifyDaysBefore(Integer notifyDaysBefore) { this.notifyDaysBefore = notifyDaysBefore; }
    public boolean isNotifyResponsible() { return notifyResponsible; }
    public void setNotifyResponsible(boolean notifyResponsible) { this.notifyResponsible = notifyResponsible; }
    public boolean isNotifySubcontractor() { return notifySubcontractor; }
    public void setNotifySubcontractor(boolean notifySubcontractor) { this.notifySubcontractor = notifySubcontractor; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
