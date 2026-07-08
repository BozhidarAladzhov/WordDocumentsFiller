package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.entities.CanadianVehicle;
import com.example.WordDocumentsFiller.entities.CanadianVehiclePaymentOrder;
import com.example.WordDocumentsFiller.entities.enums.CanadianDocumentStatus;
import com.example.WordDocumentsFiller.service.CanadianVehiclePaymentOrderMailService;
import com.example.WordDocumentsFiller.service.CanadianVehiclePaymentOrderService;
import com.example.WordDocumentsFiller.service.CanadianVehicleService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/canada-vehicles")
public class CanadianVehicleController {

    private final CanadianVehicleService canadianVehicleService;
    private final CanadianVehiclePaymentOrderService canadianVehiclePaymentOrderService;
    private final CanadianVehiclePaymentOrderMailService canadianVehiclePaymentOrderMailService;

    public CanadianVehicleController(CanadianVehicleService canadianVehicleService,
                                     CanadianVehiclePaymentOrderService canadianVehiclePaymentOrderService,
                                     CanadianVehiclePaymentOrderMailService canadianVehiclePaymentOrderMailService) {
        this.canadianVehicleService = canadianVehicleService;
        this.canadianVehiclePaymentOrderService = canadianVehiclePaymentOrderService;
        this.canadianVehiclePaymentOrderMailService = canadianVehiclePaymentOrderMailService;
    }

    @GetMapping
    public String overview(Model model) {
        model.addAttribute("vehicles", canadianVehicleService.getAll());
        model.addAttribute("newVehicle", new CanadianVehicle());
        model.addAttribute("talonStatusOptions", CanadianDocumentStatus.values());
        return "canada-vehicles/list";
    }

    @PostMapping
    public String create(@ModelAttribute("newVehicle") CanadianVehicle newVehicle) {
        canadianVehicleService.create(newVehicle);
        return "redirect:/canada-vehicles";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        CanadianVehicle vehicle = canadianVehicleService.getById(id);
        List<CanadianVehiclePaymentOrder> paymentOrders = canadianVehiclePaymentOrderService.getByVehicleId(id);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("talonStatusOptions", CanadianDocumentStatus.values());
        model.addAttribute("paymentOrders", paymentOrders);
        model.addAttribute("paymentOrderMailConfigured", canadianVehiclePaymentOrderMailService.isConfigured());
        model.addAttribute("paymentOrderMailTo", canadianVehiclePaymentOrderMailService.getTo());
        model.addAttribute("paymentOrderMailCc", canadianVehiclePaymentOrderMailService.getCc());
        model.addAttribute("paymentOrderMailSubject", canadianVehiclePaymentOrderMailService.buildSubject(vehicle));
        model.addAttribute("paymentOrderMailBody", canadianVehiclePaymentOrderMailService.buildBody(vehicle, paymentOrders));
        return "canada-vehicles/details";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @ModelAttribute("vehicle") CanadianVehicle vehicle) {
        canadianVehicleService.update(id, vehicle);
        return "redirect:/canada-vehicles/" + id;
    }

    @PostMapping("/{id}/payment-orders")
    public String uploadPaymentOrders(@PathVariable Long id,
                                      @RequestParam("files") MultipartFile[] files) throws IOException {
        CanadianVehicle vehicle = canadianVehicleService.getById(id);
        canadianVehiclePaymentOrderService.storeFiles(vehicle, files);
        return "redirect:/canada-vehicles/" + id;
    }

    @GetMapping("/{id}/payment-orders/{paymentOrderId}/download")
    public ResponseEntity<Resource> downloadPaymentOrder(@PathVariable Long id,
                                                         @PathVariable Long paymentOrderId) throws IOException {
        CanadianVehiclePaymentOrder paymentOrder = canadianVehiclePaymentOrderService.getForVehicle(id, paymentOrderId);
        Resource resource = canadianVehiclePaymentOrderService.loadAsResource(id, paymentOrderId);
        String contentType = paymentOrder.getContentType();
        MediaType mediaType = (contentType != null && !contentType.isBlank())
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + java.net.URLEncoder.encode(paymentOrder.getOriginalFileName(), StandardCharsets.UTF_8))
                .body(resource);
    }

    @PostMapping("/{id}/payment-orders/{paymentOrderId}/delete")
    public String deletePaymentOrder(@PathVariable Long id,
                                     @PathVariable Long paymentOrderId) throws IOException {
        canadianVehiclePaymentOrderService.delete(id, paymentOrderId);
        return "redirect:/canada-vehicles/" + id;
    }

    @PostMapping("/{id}/payment-orders/send")
    public String sendPaymentOrders(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        CanadianVehicle vehicle = canadianVehicleService.getById(id);
        List<CanadianVehiclePaymentOrder> paymentOrders = canadianVehiclePaymentOrderService.getByVehicleId(id);
        try {
            canadianVehiclePaymentOrderMailService.sendPaymentOrders(vehicle, paymentOrders);
            canadianVehicleService.markPaymentOrdersSent(id, canadianVehiclePaymentOrderMailService.getTo());
            redirectAttributes.addFlashAttribute("paymentOrderMailSuccess",
                    "Payment order email sent to " + canadianVehiclePaymentOrderMailService.getTo());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("paymentOrderMailError", e.getMessage());
        }
        return "redirect:/canada-vehicles/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        canadianVehicleService.delete(id);
        return "redirect:/canada-vehicles";
    }
}
