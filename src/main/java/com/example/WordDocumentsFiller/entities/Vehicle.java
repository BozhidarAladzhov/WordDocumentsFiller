package com.example.WordDocumentsFiller.entities;

import com.example.WordDocumentsFiller.entities.enums.CustomsDirection;
import com.example.WordDocumentsFiller.entities.enums.PaidStatus;
import com.example.WordDocumentsFiller.entities.enums.TitlesStatus;
import com.example.WordDocumentsFiller.entities.enums.VehicleStatus;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "container_id", nullable = false)
    private Container container;

    @Column(length = 255)
    private String description;

    @Column(length = 64)
    private String vin;

    @Column(name = "owner_name", length = 128)
    private String ownerName;

    @Column(length = 64)
    private String phone;

    @Column(length = 128)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "customs_direction", nullable = false, length = 32)
    private CustomsDirection customsDirection = CustomsDirection.IMPORT_FREELINE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TitlesStatus titles = TitlesStatus.MISSING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaidStatus paid = PaidStatus.NOT_PAID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VehicleStatus status = VehicleStatus.IN_TRANSIT;

    private LocalDate eta;


    public Long getId() { return id; }

    public Container getContainer() { return container; }
    public void setContainer(Container container) { this.container = container; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public CustomsDirection getCustomsDirection() { return customsDirection; }
    public void setCustomsDirection(CustomsDirection customsDirection) { this.customsDirection = customsDirection; }

    public TitlesStatus getTitles() { return titles; }
    public void setTitles(TitlesStatus titles) { this.titles = titles; }

    public PaidStatus getPaid() { return paid; }
    public void setPaid(PaidStatus paid) { this.paid = paid; }

    public VehicleStatus getStatus() { return status; }
    public void setStatus(VehicleStatus status) { this.status = status; }

    public LocalDate getEta() { return eta; }
    public void setEta(LocalDate eta) { this.eta = eta; }
}
