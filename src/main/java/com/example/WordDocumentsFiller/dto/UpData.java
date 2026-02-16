package com.example.WordDocumentsFiller.dto;

public class UpData {

    private Long clientId;

    private String buyerName;
    private String buyerAddress;
    private String buyerTown;
    private String buyerCountry;
    private String eori;

    private String order;
    private String mbl;
    private String container;
    private String description;
    private String descriptionBg;
    private String vin;
    private String date;

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerAddress() {
        return buyerAddress;
    }

    public void setBuyerAddress(String buyerAddress) {
        this.buyerAddress = buyerAddress;
    }

    public String getBuyerTown() {
        return buyerTown;
    }

    public void setBuyerTown(String buyerTown) {
        this.buyerTown = buyerTown;
    }

    public String getBuyerCountry() {
        return buyerCountry;
    }

    public void setBuyerCountry(String buyerCountry) {
        this.buyerCountry = buyerCountry;
    }

    public String getEori() {
        return eori;
    }

    public void setEori(String eori) {
        this.eori = eori;
    }

    public String getMbl() {
        return mbl;
    }

    public void setMbl(String mbl) {
        this.mbl = mbl;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescriptionBg() {
        return descriptionBg;
    }

    public void setDescriptionBg(String descriptionBg) {
        this.descriptionBg = descriptionBg;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
