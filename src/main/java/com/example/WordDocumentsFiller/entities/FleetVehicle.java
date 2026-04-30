package com.example.WordDocumentsFiller.entities;

import com.example.WordDocumentsFiller.entities.enums.FleetVehicleStatus;
import com.example.WordDocumentsFiller.entities.enums.InsurancePaymentPlan;
import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fleet_vehicles")
public class FleetVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", nullable = false, unique = true, length = 32)
    private String registrationNumber;

    @Column(name = "make_model", nullable = false, length = 128)
    private String makeModel;

    @Column(length = 64)
    private String vin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FleetVehicleStatus status = FleetVehicleStatus.ACTIVE;

    @Column(name = "vehicle_user_name", length = 128)
    private String vehicleUserName;

    @Column(name = "vehicle_user_email", length = 160)
    private String vehicleUserEmail;

    @Column(name = "fleet_manager_name", length = 128)
    private String fleetManagerName;

    @Column(name = "fleet_manager_email", length = 160)
    private String fleetManagerEmail;

    @Column(name = "liability_insurance_expires_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate liabilityInsuranceExpiresOn;

    @Column(name = "liability_insurer", length = 160)
    private String liabilityInsurer;

    @Column(name = "liability_price", precision = 12, scale = 2)
    private BigDecimal liabilityPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "liability_payment_plan", length = 32)
    private InsurancePaymentPlan liabilityPaymentPlan = InsurancePaymentPlan.FULL;

    @Column(name = "liability_installment_1_due_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate liabilityInstallment1DueOn;

    @Column(name = "liability_installment_1_price", precision = 12, scale = 2)
    private BigDecimal liabilityInstallment1Price;

    @Column(name = "liability_installment_1_paid", nullable = false)
    private boolean liabilityInstallment1Paid;

    @Column(name = "liability_installment_2_due_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate liabilityInstallment2DueOn;

    @Column(name = "liability_installment_2_price", precision = 12, scale = 2)
    private BigDecimal liabilityInstallment2Price;

    @Column(name = "liability_installment_2_paid", nullable = false)
    private boolean liabilityInstallment2Paid;

    @Column(name = "liability_installment_3_due_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate liabilityInstallment3DueOn;

    @Column(name = "liability_installment_3_price", precision = 12, scale = 2)
    private BigDecimal liabilityInstallment3Price;

    @Column(name = "liability_installment_3_paid", nullable = false)
    private boolean liabilityInstallment3Paid;

    @Column(name = "liability_installment_4_due_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate liabilityInstallment4DueOn;

    @Column(name = "liability_installment_4_price", precision = 12, scale = 2)
    private BigDecimal liabilityInstallment4Price;

    @Column(name = "liability_installment_4_paid", nullable = false)
    private boolean liabilityInstallment4Paid;

    @Column(name = "casco_insurance_expires_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate cascoInsuranceExpiresOn;

    @Column(name = "casco_insurer", length = 160)
    private String cascoInsurer;

    @Column(name = "casco_price", precision = 12, scale = 2)
    private BigDecimal cascoPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "casco_payment_plan", length = 32)
    private InsurancePaymentPlan cascoPaymentPlan = InsurancePaymentPlan.FULL;

    @Column(name = "casco_installment_1_due_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate cascoInstallment1DueOn;

    @Column(name = "casco_installment_1_price", precision = 12, scale = 2)
    private BigDecimal cascoInstallment1Price;

    @Column(name = "casco_installment_1_paid", nullable = false)
    private boolean cascoInstallment1Paid;

    @Column(name = "casco_installment_2_due_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate cascoInstallment2DueOn;

    @Column(name = "casco_installment_2_price", precision = 12, scale = 2)
    private BigDecimal cascoInstallment2Price;

    @Column(name = "casco_installment_2_paid", nullable = false)
    private boolean cascoInstallment2Paid;

    @Column(name = "casco_installment_3_due_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate cascoInstallment3DueOn;

    @Column(name = "casco_installment_3_price", precision = 12, scale = 2)
    private BigDecimal cascoInstallment3Price;

    @Column(name = "casco_installment_3_paid", nullable = false)
    private boolean cascoInstallment3Paid;

    @Column(name = "casco_installment_4_due_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate cascoInstallment4DueOn;

    @Column(name = "casco_installment_4_price", precision = 12, scale = 2)
    private BigDecimal cascoInstallment4Price;

    @Column(name = "casco_installment_4_paid", nullable = false)
    private boolean cascoInstallment4Paid;

    @Column(name = "technical_inspection_expires_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate technicalInspectionExpiresOn;

    @Column(name = "technical_inspection_price", precision = 12, scale = 2)
    private BigDecimal technicalInspectionPrice;

    @Column(name = "vignette_expires_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate vignetteExpiresOn;

    @Column(name = "vignette_price", precision = 12, scale = 2)
    private BigDecimal vignettePrice;

    @Column(name = "service_last_done_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate serviceLastDoneOn;

    @Column(name = "service_due_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate serviceDueOn;

    @Column(name = "tire_change_due_on")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate tireChangeDueOn;

    @Column(name = "summer_tires_active", nullable = false)
    private boolean summerTiresActive;

    @Column(name = "summer_tires_brand", length = 64)
    private String summerTiresBrand;

    @Column(name = "summer_tires_size", length = 64)
    private String summerTiresSize;

    @Column(name = "summer_tires_dot", length = 32)
    private String summerTiresDot;

    @Column(name = "winter_tires_active", nullable = false)
    private boolean winterTiresActive;

    @Column(name = "winter_tires_brand", length = 64)
    private String winterTiresBrand;

    @Column(name = "winter_tires_size", length = 64)
    private String winterTiresSize;

    @Column(name = "winter_tires_dot", length = 32)
    private String winterTiresDot;

    @Column(name = "all_season_tires_active", nullable = false)
    private boolean allSeasonTiresActive;

    @Column(name = "all_season_tires_brand", length = 64)
    private String allSeasonTiresBrand;

    @Column(name = "all_season_tires_size", length = 64)
    private String allSeasonTiresSize;

    @Column(name = "all_season_tires_dot", length = 32)
    private String allSeasonTiresDot;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FleetEvent> events = new ArrayList<>();

    public Long getId() { return id; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getMakeModel() { return makeModel; }
    public void setMakeModel(String makeModel) { this.makeModel = makeModel; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public FleetVehicleStatus getStatus() { return status; }
    public void setStatus(FleetVehicleStatus status) { this.status = status; }
    public String getVehicleUserName() { return vehicleUserName; }
    public void setVehicleUserName(String vehicleUserName) { this.vehicleUserName = vehicleUserName; }
    public String getVehicleUserEmail() { return vehicleUserEmail; }
    public void setVehicleUserEmail(String vehicleUserEmail) { this.vehicleUserEmail = vehicleUserEmail; }
    public String getFleetManagerName() { return fleetManagerName; }
    public void setFleetManagerName(String fleetManagerName) { this.fleetManagerName = fleetManagerName; }
    public String getFleetManagerEmail() { return fleetManagerEmail; }
    public void setFleetManagerEmail(String fleetManagerEmail) { this.fleetManagerEmail = fleetManagerEmail; }
    public LocalDate getLiabilityInsuranceExpiresOn() { return liabilityInsuranceExpiresOn; }
    public void setLiabilityInsuranceExpiresOn(LocalDate liabilityInsuranceExpiresOn) { this.liabilityInsuranceExpiresOn = liabilityInsuranceExpiresOn; }
    public String getLiabilityInsurer() { return liabilityInsurer; }
    public void setLiabilityInsurer(String liabilityInsurer) { this.liabilityInsurer = liabilityInsurer; }
    public BigDecimal getLiabilityPrice() { return liabilityPrice; }
    public void setLiabilityPrice(BigDecimal liabilityPrice) { this.liabilityPrice = liabilityPrice; }
    public InsurancePaymentPlan getLiabilityPaymentPlan() { return liabilityPaymentPlan; }
    public void setLiabilityPaymentPlan(InsurancePaymentPlan liabilityPaymentPlan) { this.liabilityPaymentPlan = liabilityPaymentPlan; }
    public LocalDate getLiabilityInstallment1DueOn() { return liabilityInstallment1DueOn; }
    public void setLiabilityInstallment1DueOn(LocalDate liabilityInstallment1DueOn) { this.liabilityInstallment1DueOn = liabilityInstallment1DueOn; }
    public BigDecimal getLiabilityInstallment1Price() { return liabilityInstallment1Price; }
    public void setLiabilityInstallment1Price(BigDecimal liabilityInstallment1Price) { this.liabilityInstallment1Price = liabilityInstallment1Price; }
    public boolean isLiabilityInstallment1Paid() { return liabilityInstallment1Paid; }
    public void setLiabilityInstallment1Paid(boolean liabilityInstallment1Paid) { this.liabilityInstallment1Paid = liabilityInstallment1Paid; }
    public LocalDate getLiabilityInstallment2DueOn() { return liabilityInstallment2DueOn; }
    public void setLiabilityInstallment2DueOn(LocalDate liabilityInstallment2DueOn) { this.liabilityInstallment2DueOn = liabilityInstallment2DueOn; }
    public BigDecimal getLiabilityInstallment2Price() { return liabilityInstallment2Price; }
    public void setLiabilityInstallment2Price(BigDecimal liabilityInstallment2Price) { this.liabilityInstallment2Price = liabilityInstallment2Price; }
    public boolean isLiabilityInstallment2Paid() { return liabilityInstallment2Paid; }
    public void setLiabilityInstallment2Paid(boolean liabilityInstallment2Paid) { this.liabilityInstallment2Paid = liabilityInstallment2Paid; }
    public LocalDate getLiabilityInstallment3DueOn() { return liabilityInstallment3DueOn; }
    public void setLiabilityInstallment3DueOn(LocalDate liabilityInstallment3DueOn) { this.liabilityInstallment3DueOn = liabilityInstallment3DueOn; }
    public BigDecimal getLiabilityInstallment3Price() { return liabilityInstallment3Price; }
    public void setLiabilityInstallment3Price(BigDecimal liabilityInstallment3Price) { this.liabilityInstallment3Price = liabilityInstallment3Price; }
    public boolean isLiabilityInstallment3Paid() { return liabilityInstallment3Paid; }
    public void setLiabilityInstallment3Paid(boolean liabilityInstallment3Paid) { this.liabilityInstallment3Paid = liabilityInstallment3Paid; }
    public LocalDate getLiabilityInstallment4DueOn() { return liabilityInstallment4DueOn; }
    public void setLiabilityInstallment4DueOn(LocalDate liabilityInstallment4DueOn) { this.liabilityInstallment4DueOn = liabilityInstallment4DueOn; }
    public BigDecimal getLiabilityInstallment4Price() { return liabilityInstallment4Price; }
    public void setLiabilityInstallment4Price(BigDecimal liabilityInstallment4Price) { this.liabilityInstallment4Price = liabilityInstallment4Price; }
    public boolean isLiabilityInstallment4Paid() { return liabilityInstallment4Paid; }
    public void setLiabilityInstallment4Paid(boolean liabilityInstallment4Paid) { this.liabilityInstallment4Paid = liabilityInstallment4Paid; }
    public LocalDate getCascoInsuranceExpiresOn() { return cascoInsuranceExpiresOn; }
    public void setCascoInsuranceExpiresOn(LocalDate cascoInsuranceExpiresOn) { this.cascoInsuranceExpiresOn = cascoInsuranceExpiresOn; }
    public String getCascoInsurer() { return cascoInsurer; }
    public void setCascoInsurer(String cascoInsurer) { this.cascoInsurer = cascoInsurer; }
    public BigDecimal getCascoPrice() { return cascoPrice; }
    public void setCascoPrice(BigDecimal cascoPrice) { this.cascoPrice = cascoPrice; }
    public InsurancePaymentPlan getCascoPaymentPlan() { return cascoPaymentPlan; }
    public void setCascoPaymentPlan(InsurancePaymentPlan cascoPaymentPlan) { this.cascoPaymentPlan = cascoPaymentPlan; }
    public LocalDate getCascoInstallment1DueOn() { return cascoInstallment1DueOn; }
    public void setCascoInstallment1DueOn(LocalDate cascoInstallment1DueOn) { this.cascoInstallment1DueOn = cascoInstallment1DueOn; }
    public BigDecimal getCascoInstallment1Price() { return cascoInstallment1Price; }
    public void setCascoInstallment1Price(BigDecimal cascoInstallment1Price) { this.cascoInstallment1Price = cascoInstallment1Price; }
    public boolean isCascoInstallment1Paid() { return cascoInstallment1Paid; }
    public void setCascoInstallment1Paid(boolean cascoInstallment1Paid) { this.cascoInstallment1Paid = cascoInstallment1Paid; }
    public LocalDate getCascoInstallment2DueOn() { return cascoInstallment2DueOn; }
    public void setCascoInstallment2DueOn(LocalDate cascoInstallment2DueOn) { this.cascoInstallment2DueOn = cascoInstallment2DueOn; }
    public BigDecimal getCascoInstallment2Price() { return cascoInstallment2Price; }
    public void setCascoInstallment2Price(BigDecimal cascoInstallment2Price) { this.cascoInstallment2Price = cascoInstallment2Price; }
    public boolean isCascoInstallment2Paid() { return cascoInstallment2Paid; }
    public void setCascoInstallment2Paid(boolean cascoInstallment2Paid) { this.cascoInstallment2Paid = cascoInstallment2Paid; }
    public LocalDate getCascoInstallment3DueOn() { return cascoInstallment3DueOn; }
    public void setCascoInstallment3DueOn(LocalDate cascoInstallment3DueOn) { this.cascoInstallment3DueOn = cascoInstallment3DueOn; }
    public BigDecimal getCascoInstallment3Price() { return cascoInstallment3Price; }
    public void setCascoInstallment3Price(BigDecimal cascoInstallment3Price) { this.cascoInstallment3Price = cascoInstallment3Price; }
    public boolean isCascoInstallment3Paid() { return cascoInstallment3Paid; }
    public void setCascoInstallment3Paid(boolean cascoInstallment3Paid) { this.cascoInstallment3Paid = cascoInstallment3Paid; }
    public LocalDate getCascoInstallment4DueOn() { return cascoInstallment4DueOn; }
    public void setCascoInstallment4DueOn(LocalDate cascoInstallment4DueOn) { this.cascoInstallment4DueOn = cascoInstallment4DueOn; }
    public BigDecimal getCascoInstallment4Price() { return cascoInstallment4Price; }
    public void setCascoInstallment4Price(BigDecimal cascoInstallment4Price) { this.cascoInstallment4Price = cascoInstallment4Price; }
    public boolean isCascoInstallment4Paid() { return cascoInstallment4Paid; }
    public void setCascoInstallment4Paid(boolean cascoInstallment4Paid) { this.cascoInstallment4Paid = cascoInstallment4Paid; }
    public LocalDate getTechnicalInspectionExpiresOn() { return technicalInspectionExpiresOn; }
    public void setTechnicalInspectionExpiresOn(LocalDate technicalInspectionExpiresOn) { this.technicalInspectionExpiresOn = technicalInspectionExpiresOn; }
    public BigDecimal getTechnicalInspectionPrice() { return technicalInspectionPrice; }
    public void setTechnicalInspectionPrice(BigDecimal technicalInspectionPrice) { this.technicalInspectionPrice = technicalInspectionPrice; }
    public LocalDate getVignetteExpiresOn() { return vignetteExpiresOn; }
    public void setVignetteExpiresOn(LocalDate vignetteExpiresOn) { this.vignetteExpiresOn = vignetteExpiresOn; }
    public BigDecimal getVignettePrice() { return vignettePrice; }
    public void setVignettePrice(BigDecimal vignettePrice) { this.vignettePrice = vignettePrice; }
    public LocalDate getServiceLastDoneOn() { return serviceLastDoneOn; }
    public void setServiceLastDoneOn(LocalDate serviceLastDoneOn) { this.serviceLastDoneOn = serviceLastDoneOn; }
    public LocalDate getServiceDueOn() { return serviceDueOn; }
    public void setServiceDueOn(LocalDate serviceDueOn) { this.serviceDueOn = serviceDueOn; }
    public LocalDate getTireChangeDueOn() { return tireChangeDueOn; }
    public void setTireChangeDueOn(LocalDate tireChangeDueOn) { this.tireChangeDueOn = tireChangeDueOn; }
    public boolean isSummerTiresActive() { return summerTiresActive; }
    public void setSummerTiresActive(boolean summerTiresActive) { this.summerTiresActive = summerTiresActive; }
    public String getSummerTiresBrand() { return summerTiresBrand; }
    public void setSummerTiresBrand(String summerTiresBrand) { this.summerTiresBrand = summerTiresBrand; }
    public String getSummerTiresSize() { return summerTiresSize; }
    public void setSummerTiresSize(String summerTiresSize) { this.summerTiresSize = summerTiresSize; }
    public String getSummerTiresDot() { return summerTiresDot; }
    public void setSummerTiresDot(String summerTiresDot) { this.summerTiresDot = summerTiresDot; }
    public boolean isWinterTiresActive() { return winterTiresActive; }
    public void setWinterTiresActive(boolean winterTiresActive) { this.winterTiresActive = winterTiresActive; }
    public String getWinterTiresBrand() { return winterTiresBrand; }
    public void setWinterTiresBrand(String winterTiresBrand) { this.winterTiresBrand = winterTiresBrand; }
    public String getWinterTiresSize() { return winterTiresSize; }
    public void setWinterTiresSize(String winterTiresSize) { this.winterTiresSize = winterTiresSize; }
    public String getWinterTiresDot() { return winterTiresDot; }
    public void setWinterTiresDot(String winterTiresDot) { this.winterTiresDot = winterTiresDot; }
    public boolean isAllSeasonTiresActive() { return allSeasonTiresActive; }
    public void setAllSeasonTiresActive(boolean allSeasonTiresActive) { this.allSeasonTiresActive = allSeasonTiresActive; }
    public String getAllSeasonTiresBrand() { return allSeasonTiresBrand; }
    public void setAllSeasonTiresBrand(String allSeasonTiresBrand) { this.allSeasonTiresBrand = allSeasonTiresBrand; }
    public String getAllSeasonTiresSize() { return allSeasonTiresSize; }
    public void setAllSeasonTiresSize(String allSeasonTiresSize) { this.allSeasonTiresSize = allSeasonTiresSize; }
    public String getAllSeasonTiresDot() { return allSeasonTiresDot; }
    public void setAllSeasonTiresDot(String allSeasonTiresDot) { this.allSeasonTiresDot = allSeasonTiresDot; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<FleetEvent> getEvents() { return events; }
}
