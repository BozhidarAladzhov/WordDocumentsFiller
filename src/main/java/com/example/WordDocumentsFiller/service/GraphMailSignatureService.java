package com.example.WordDocumentsFiller.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class GraphMailSignatureService {

    public String buildHtmlBody(String plainBody) {
        return buildHtmlBody(plainBody, null);
    }

    public String buildHtmlBody(String plainBody, String senderEmail) {
        String normalized = plainBody == null ? "" : plainBody.replace("\r\n", "\n");
        String bodyContent = looksLikeHtml(normalized)
                ? normalized
                : escapeHtml(normalized).replace("\n", "<br>");
        String signature = signatureFor(senderEmail);

        return """
                <div style="font-family:'Verdana Pro', Verdana, Arial, sans-serif; font-size:10pt; line-height:1.45; color:#333333;">
                  %s
                  <br><br>
                  %s
                </div>
                """.formatted(bodyContent, signature);
    }

    private String signatureFor(String senderEmail) {
        String normalizedEmail = senderEmail == null ? "" : senderEmail.trim().toLowerCase(Locale.ROOT);
        if ("petya@freeline.bg".equals(normalizedEmail)) {
            return """
                    <div style="font-size:10pt; font-family:'Verdana Pro', Verdana, Arial, sans-serif; color:#333333; line-height:1.45;">
                      -- <br>
                      Best regards<br>
                      Petya Zayakova<br>
                      <img src="cid:freeline-banner" alt="Freeline" width="378" height="42" style="display:block; width:10cm; height:1.11cm; margin:6px 0 8px 0; border:0;"><br>
                      92 G, Iliyantsi Blvd.,<br>
                      1220 Sofia, Bulgaria<br>
                      Tel: +3592 442 01 88<br>
                      GSM +359 895524295<br>
                      e-mail: <a href="mailto:petya@freeline.bg" style="color:#333333; text-decoration:none;">petya@freeline.bg</a><br>
                      <a href="https://www.freeline.bg" style="color:#333333; text-decoration:none;">freeline.bg</a><br>
                      <div style="margin-top:10px;">
                        <img src="cid:freeline-logo-small" alt="Freeline Mark" width="116" height="56" style="display:inline-block; width:3.07cm; height:1.48cm; border:0; vertical-align:middle; margin-right:10px;">
                        <img src="cid:freeline-logo-wide" alt="Freeline Logistics" width="90" height="57" style="display:inline-block; width:2.38cm; height:1.51cm; border:0; vertical-align:middle;">
                      </div>
                    </div>
                    """;
        }
        return """
                <div style="font-size:10pt; font-family:'Verdana Pro', Verdana, Arial, sans-serif; color:#333333; line-height:1.45;">
                  -- <br>
                  Best regards,<br>
                  Bojidar Aladjov<br>
                  <img src="cid:freeline-banner" alt="Freeline" width="378" height="42" style="display:block; width:10cm; height:1.11cm; margin:6px 0 8px 0; border:0;"><br>
                  International Transport and Forwarding<br><br>
                  92 G, Iliyantsi Blvd.,<br>
                  1220 Sofia, Bulgaria<br>
                  Phone : +359 2 915 11 55<br>
                  Mobile: +359 879 233 698<br>
                  E-MAIL: <a href="mailto:bojidar@freeline.bg" style="color:#333333; text-decoration:none;">bojidar@freeline.bg</a><br>
                  <a href="https://www.freeline.bg" style="color:#333333; text-decoration:none;">www.freeline.bg</a><br>
                  <div style="margin-top:10px;">
                    <img src="cid:freeline-logo-small" alt="Freeline Mark" width="116" height="56" style="display:inline-block; width:3.07cm; height:1.48cm; border:0; vertical-align:middle; margin-right:10px;">
                    <img src="cid:freeline-logo-wide" alt="Freeline Logistics" width="90" height="57" style="display:inline-block; width:2.38cm; height:1.51cm; border:0; vertical-align:middle;">
                  </div>
                </div>
                """;
    }

    public List<InlineAttachment> inlineAttachments() {
        return List.of(
                loadAttachment("mail/signature/image001.jpg", "image/jpeg", "freeline-banner", "freeline-banner.jpg"),
                loadAttachment("mail/signature/image002.png", "image/png", "freeline-logo-small", "freeline-logo-small.png"),
                loadAttachment("mail/signature/image003.png", "image/png", "freeline-logo-wide", "freeline-logo-wide.png")
        );
    }

    private InlineAttachment loadAttachment(String classpathPath,
                                            String contentType,
                                            String contentId,
                                            String name) {
        try {
            byte[] bytes = new ClassPathResource(classpathPath).getInputStream().readAllBytes();
            return new InlineAttachment(
                    contentType,
                    contentId,
                    name,
                    Base64.getEncoder().encodeToString(bytes)
            );
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load signature asset: " + classpathPath, e);
        }
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static boolean looksLikeHtml(String value) {
        String lower = value == null ? "" : value.toLowerCase();
        return lower.contains("<table") || lower.contains("<p>") || lower.contains("<br");
    }

    public record InlineAttachment(
            String contentType,
            String contentId,
            String name,
            String contentBytes
    ) {
    }
}
