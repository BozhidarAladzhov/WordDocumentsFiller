package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.CanadianVehicle;
import com.example.WordDocumentsFiller.entities.CanadianVehiclePaymentOrder;
import com.example.WordDocumentsFiller.repositories.CanadianVehiclePaymentOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CanadianVehiclePaymentOrderService {

    private static final Logger log = LoggerFactory.getLogger(CanadianVehiclePaymentOrderService.class);

    private final CanadianVehiclePaymentOrderRepository paymentOrderRepository;
    private final CanadianPaymentOrderStorageProperties storageProperties;

    public CanadianVehiclePaymentOrderService(CanadianVehiclePaymentOrderRepository paymentOrderRepository,
                                              CanadianPaymentOrderStorageProperties storageProperties) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.storageProperties = storageProperties;
    }

    public List<CanadianVehiclePaymentOrder> getByVehicleId(Long vehicleId) {
        return paymentOrderRepository.findByCanadianVehicleIdOrderByUploadedAtAscIdAsc(vehicleId);
    }

    public List<CanadianVehiclePaymentOrder> storeFiles(CanadianVehicle vehicle,
                                                        MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) return List.of();

        Path baseDir = resolveBaseDir();
        Path vehicleDir = baseDir.resolve(buildVehicleFolderName(vehicle));
        log.info("Payment order upload started: vehicleId={}, baseDir={}, vehicleDir={}",
                vehicle.getId(), baseDir, vehicleDir);
        Files.createDirectories(vehicleDir);

        List<CanadianVehiclePaymentOrder> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            String originalFileName = sanitizeFileName(file.getOriginalFilename());
            String extension = extractExtension(originalFileName);
            String storedFileName = UUID.randomUUID() + extension;
            Path target = vehicleDir.resolve(storedFileName).normalize();

            log.info("Saving payment order file: vehicleId={}, originalFileName={}, sizeBytes={}, target={}",
                    vehicle.getId(), originalFileName, file.getSize(), target);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved payment order file: vehicleId={}, target={}, exists={}",
                    vehicle.getId(), target, Files.exists(target));

            CanadianVehiclePaymentOrder paymentOrder = new CanadianVehiclePaymentOrder();
            paymentOrder.setCanadianVehicle(vehicle);
            paymentOrder.setOriginalFileName(originalFileName);
            paymentOrder.setStoredFileName(storedFileName);
            paymentOrder.setContentType(StringUtils.hasText(file.getContentType()) ? file.getContentType() : Files.probeContentType(target));
            paymentOrder.setFileSizeBytes(file.getSize());
            paymentOrder.setStorageRelativePath(baseDir.relativize(target).toString());
            paymentOrder.setUploadedAt(LocalDateTime.now());
            saved.add(paymentOrderRepository.save(paymentOrder));
            log.info("Saved payment order metadata: vehicleId={}, paymentOrderId={}, relativePath={}",
                    vehicle.getId(), saved.get(saved.size() - 1).getId(), paymentOrder.getStorageRelativePath());
        }
        return saved;
    }

    public CanadianVehiclePaymentOrder getForVehicle(Long vehicleId, Long paymentOrderId) {
        CanadianVehiclePaymentOrder paymentOrder = paymentOrderRepository.findById(paymentOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment order not found: " + paymentOrderId));

        if (paymentOrder.getCanadianVehicle() == null
                || paymentOrder.getCanadianVehicle().getId() == null
                || !paymentOrder.getCanadianVehicle().getId().equals(vehicleId)) {
            throw new IllegalArgumentException("Payment order " + paymentOrderId + " does not belong to vehicle " + vehicleId);
        }
        return paymentOrder;
    }

    public Path resolveStoredFilePath(CanadianVehiclePaymentOrder paymentOrder) {
        return resolveBaseDir().resolve(paymentOrder.getStorageRelativePath()).normalize();
    }

    public boolean storedFileExists(CanadianVehiclePaymentOrder paymentOrder) {
        return Files.exists(resolveStoredFilePath(paymentOrder));
    }

    public Resource loadAsResource(Long vehicleId, Long paymentOrderId) throws MalformedURLException {
        CanadianVehiclePaymentOrder paymentOrder = getForVehicle(vehicleId, paymentOrderId);
        Path filePath = resolveBaseDir().resolve(paymentOrder.getStorageRelativePath()).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException("Stored file is missing for payment order " + paymentOrderId);
        }
        return resource;
    }

    public void delete(Long vehicleId, Long paymentOrderId) throws IOException {
        CanadianVehiclePaymentOrder paymentOrder = getForVehicle(vehicleId, paymentOrderId);
        Path filePath = resolveBaseDir().resolve(paymentOrder.getStorageRelativePath()).normalize();
        Files.deleteIfExists(filePath);
        paymentOrderRepository.delete(paymentOrder);
    }

    private Path resolveBaseDir() {
        if (!StringUtils.hasText(storageProperties.getStorageDir())) {
            throw new IllegalStateException("canada.payment-orders.storage-dir is not configured");
        }
        return Paths.get(storageProperties.getStorageDir()).toAbsolutePath().normalize();
    }

    private static String sanitizeFileName(String originalFileName) {
        String cleaned = StringUtils.cleanPath(originalFileName == null ? "" : originalFileName).replace('\\', '/');
        int slashIndex = cleaned.lastIndexOf('/');
        String baseName = slashIndex >= 0 ? cleaned.substring(slashIndex + 1) : cleaned;
        return StringUtils.hasText(baseName) ? baseName : "file";
    }

    private static String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot);
    }

    private static String buildVehicleFolderName(CanadianVehicle vehicle) {
        String description = sanitizeFolderSegment(vehicle.getVehicleName());
        String vin = sanitizeFolderSegment(vehicle.getVin());

        if (!StringUtils.hasText(description)) {
            description = "Vehicle";
        }
        if (!StringUtils.hasText(vin)) {
            vin = vehicle.getId() != null ? String.valueOf(vehicle.getId()) : "Unknown";
        }

        return description + " VIN " + vin;
    }

    private static String sanitizeFolderSegment(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String sanitized = value.trim()
                .replaceAll("[\\\\/:*?\"<>|]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (sanitized.endsWith(".")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1).trim();
        }

        return sanitized;
    }
}
