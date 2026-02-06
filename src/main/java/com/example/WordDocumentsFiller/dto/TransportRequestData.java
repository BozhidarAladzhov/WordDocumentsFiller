package com.example.WordDocumentsFiller.dto;

public class TransportRequestData {

    private String transportCompany;
    private String date;
    private String transportCompanyVAT;

    private String truck;

    private String loadingAddress;
    private String dateOfLoading;
    private String dateOfDelivery;

    private Integer vehiclesCount;
    private String vehiclesList;

    private String price;

    public String getTransportCompany() {
        return transportCompany;
    }

    public void setTransportCompany(String transportCompany) {
        this.transportCompany = transportCompany;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTransportCompanyVAT() {
        return transportCompanyVAT;
    }

    public void setTransportCompanyVAT(String transportCompanyVAT) {
        this.transportCompanyVAT = transportCompanyVAT;
    }

    public String getTruck() {
        return truck;
    }

    public void setTruck(String truck) {
        this.truck = truck;
    }

    public String getLoadingAddress() {
        return loadingAddress;
    }

    public void setLoadingAddress(String loadingAddress) {
        this.loadingAddress = loadingAddress;
    }

    public String getDateOfLoading() {
        return dateOfLoading;
    }

    public void setDateOfLoading(String dateOfLoading) {
        this.dateOfLoading = dateOfLoading;
    }

    public String getDateOfDelivery() {
        return dateOfDelivery;
    }

    public void setDateOfDelivery(String dateOfDelivery) {
        this.dateOfDelivery = dateOfDelivery;
    }

    public Integer getVehiclesCount() {
        return vehiclesCount;
    }

    public void setVehiclesCount(Integer vehiclesCount) {
        this.vehiclesCount = vehiclesCount;
    }

    public String getVehiclesList() {
        return vehiclesList;
    }

    public void setVehiclesList(String vehiclesList) {
        this.vehiclesList = vehiclesList;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}
