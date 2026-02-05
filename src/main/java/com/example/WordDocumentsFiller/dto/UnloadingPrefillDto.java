package com.example.WordDocumentsFiller.dto;

import java.util.List;

public class UnloadingPrefillDto {

    private String containerNo;
    private List<VehicleDto> vehicles;

    public UnloadingPrefillDto(String containerNo, List<VehicleDto> vehicles) {
        this.containerNo = containerNo;
        this.vehicles = vehicles;
    }

    public String getContainerNo() { return containerNo; }
    public List<VehicleDto> getVehicles() { return vehicles; }

    public static class VehicleDto {
        private String car;
        private boolean canPickup;
        private boolean hasDocs;
        private boolean clientPickup;

        public VehicleDto(String car, boolean canPickup, boolean hasDocs, boolean clientPickup) {
            this.car = car;
            this.canPickup = canPickup;
            this.hasDocs = hasDocs;
            this.clientPickup = clientPickup;
        }

        public String getCar() { return car; }
        public boolean isCanPickup() { return canPickup; }
        public boolean isHasDocs() { return hasDocs; }
        public boolean isClientPickup() { return clientPickup; }
    }

}
