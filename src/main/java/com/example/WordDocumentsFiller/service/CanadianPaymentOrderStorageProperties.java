package com.example.WordDocumentsFiller.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "canada.payment-orders")
public class CanadianPaymentOrderStorageProperties {

    /**
     * Absolute path to a local directory, ideally inside OneDrive sync folder.
     */
    private String storageDir;

    public String getStorageDir() { return storageDir; }
    public void setStorageDir(String storageDir) { this.storageDir = storageDir; }
}
