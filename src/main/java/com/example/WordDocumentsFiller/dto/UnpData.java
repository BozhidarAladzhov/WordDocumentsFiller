package com.example.WordDocumentsFiller.dto;

public class UnpData {

    private String transportCompany;
    private String transportCompanyVAT;


    private String truck;
    private String trailer;

    private String arrivalDate;

    private String arrivalTime;

    public String getTransportCompany() {
        return transportCompany;
    }

    public void setTransportCompany(String transportCompany) {
        this.transportCompany = transportCompany;
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

    public String getTrailer() {
        return trailer;
    }

    public void setTrailer(String trailer) {
        this.trailer = trailer;
    }

    public String getArrivalDate() {
        return arrivalDate;
    }

    public void setArrivalDate(String arrivalDate) {
        this.arrivalDate = arrivalDate;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }
}
