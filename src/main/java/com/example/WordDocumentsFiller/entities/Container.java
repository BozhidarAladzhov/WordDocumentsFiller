package com.example.WordDocumentsFiller.entities;

import com.example.WordDocumentsFiller.entities.enums.ContainerStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "containers")
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "container_no", nullable = false, unique = true, length = 32)
    private String containerNo;

    @Column(length = 64)
    private String bol;

    @Column(length = 64)
    private String swb;

    @Column(length = 64)
    private String carrier;

    @Column(name = "vessel_name", length = 128)
    private String vesselName;

    @Column(length = 64)
    private String pol;

    @Column(length = 64)
    private String pod;

    @Column(length = 64)
    private String seal;

    @Column(name = "shipped_on_board", length = 64)
    private String shippedOnBoard;

    private LocalDate eta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContainerStatus status = ContainerStatus.IN_TRANSIT;

    @Column(name = "unloading_at", length = 128)
    private String unloadingAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "container", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vehicle> vehicles = new ArrayList<>();


    public Long getId() { return id; }

    public String getContainerNo() { return containerNo; }
    public void setContainerNo(String containerNo) { this.containerNo = containerNo; }

    public String getBol() { return bol; }
    public void setBol(String bol) { this.bol = bol; }

    public String getSwb() { return swb; }
    public void setSwb(String swb) { this.swb = swb; }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public String getVesselName() { return vesselName; }
    public void setVesselName(String vesselName) { this.vesselName = vesselName; }

    public String getPol() { return pol; }
    public void setPol(String pol) { this.pol = pol; }

    public String getPod() { return pod; }
    public void setPod(String pod) { this.pod = pod; }

    public String getSeal() { return seal; }
    public void setSeal(String seal) { this.seal = seal; }

    public String getShippedOnBoard() { return shippedOnBoard; }
    public void setShippedOnBoard(String shippedOnBoard) { this.shippedOnBoard = shippedOnBoard; }

    public LocalDate getEta() { return eta; }
    public void setEta(LocalDate eta) { this.eta = eta; }

    public ContainerStatus getStatus() { return status; }
    public void setStatus(ContainerStatus status) { this.status = status; }

    public String getUnloadingAt() { return unloadingAt; }
    public void setUnloadingAt(String unloadingAt) { this.unloadingAt = unloadingAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<Vehicle> getVehicles() { return vehicles; }
}
