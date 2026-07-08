package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.entities.CanadianVehicle;
import com.example.WordDocumentsFiller.entities.CanadianVehiclePaymentOrder;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CanadianVehiclePaymentOrderMailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final CanadaPaymentOrderMailProperties paymentOrderMailProperties;
    private final CanadianVehiclePaymentOrderService paymentOrderService;

    public CanadianVehiclePaymentOrderMailService(JavaMailSender mailSender,
                                                  MailProperties mailProperties,
                                                  CanadaPaymentOrderMailProperties paymentOrderMailProperties,
                                                  CanadianVehiclePaymentOrderService paymentOrderService) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.paymentOrderMailProperties = paymentOrderMailProperties;
        this.paymentOrderService = paymentOrderService;
    }

    public boolean isConfigured() {
        return paymentOrderMailProperties.isEnabled()
                && StringUtils.hasText(resolveFromAddress())
                && StringUtils.hasText(paymentOrderMailProperties.getTo())
                && StringUtils.hasText(mailProperties.getHost())
                && mailProperties.getPort() != null
                && StringUtils.hasText(mailProperties.getUsername())
                && StringUtils.hasText(mailProperties.getPassword());
    }

    public String getTo() {
        return paymentOrderMailProperties.getTo();
    }

    public String getCc() {
        return paymentOrderMailProperties.getCc();
    }

    public String buildSubject(CanadianVehicle vehicle) {
        String template = StringUtils.hasText(paymentOrderMailProperties.getSubjectTemplate())
                ? paymentOrderMailProperties.getSubjectTemplate()
                : "Payment orders - {vehicle} - VIN {vin}";

        return template
                .replace("{client}", safe(vehicle.getClientName()))
                .replace("{vehicle}", safe(vehicle.getVehicleName()))
                .replace("{vin}", safe(vehicle.getVin()))
                .replace("{container}", safe(vehicle.getContainerNo()));
    }

    public String buildBody(CanadianVehicle vehicle, List<CanadianVehiclePaymentOrder> paymentOrders) {
        StringBuilder body = new StringBuilder();
        body.append("Hello,").append("\n\n");
        body.append("Please find attached the payment orders for the subject vehicle:").append("\n\n");
        body.append("\n");
        body.append("Attached documents: ").append(paymentOrders.size()).append("\n");
        return body.toString();
    }

    public void sendPaymentOrders(CanadianVehicle vehicle, List<CanadianVehiclePaymentOrder> paymentOrders) {
        if (paymentOrders == null || paymentOrders.isEmpty()) {
            throw new IllegalStateException("No payment orders uploaded for this vehicle.");
        }
        if (!isConfigured()) {
            throw new IllegalStateException("Gmail SMTP is not configured. Check canada.payment-orders.mail.* and spring.mail.* settings.");
        }

        List<CanadianVehiclePaymentOrder> missingFiles = paymentOrders.stream()
                .filter(paymentOrder -> !paymentOrderService.storedFileExists(paymentOrder))
                .toList();
        if (!missingFiles.isEmpty()) {
            String missingNames = missingFiles.stream()
                    .map(CanadianVehiclePaymentOrder::getOriginalFileName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("unknown");
            throw new IllegalStateException("Missing stored attachment files: " + missingNames);
        }

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            String fromAddress = resolveFromAddress();
            helper.setFrom(fromAddress);
            helper.setTo(paymentOrderMailProperties.getTo());
            if (StringUtils.hasText(paymentOrderMailProperties.getCc())) {
                helper.setCc(splitAddresses(paymentOrderMailProperties.getCc()));
            }
            helper.setSubject(buildSubject(vehicle));
            helper.setText(buildBody(vehicle, paymentOrders), false);

            for (CanadianVehiclePaymentOrder paymentOrder : paymentOrders) {
                Path filePath = paymentOrderService.resolveStoredFilePath(paymentOrder);
                helper.addAttachment(paymentOrder.getOriginalFileName(), new FileSystemResource(filePath));
            }

            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send payment order email: " + e.getMessage(), e);
        }
    }

    private String resolveFromAddress() {
        if (StringUtils.hasText(paymentOrderMailProperties.getFromAddress())) {
            return paymentOrderMailProperties.getFromAddress();
        }
        return mailProperties.getUsername();
    }

    private static String[] splitAddresses(String addresses) {
        return List.of(addresses.split("[,;]")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }

    private static void appendLine(StringBuilder body, String label, String value) {
        if (StringUtils.hasText(value)) {
            body.append(label).append(": ").append(value).append("\n");
        }
    }

    private static String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
