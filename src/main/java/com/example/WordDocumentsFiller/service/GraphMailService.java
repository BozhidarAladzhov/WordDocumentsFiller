package com.example.WordDocumentsFiller.service;

import com.example.WordDocumentsFiller.dto.GraphTokenSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class GraphMailService {

    private final GraphMailProperties properties;
    private final ObjectMapper objectMapper;
    private final GraphMailSignatureService graphMailSignatureService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GraphMailService(GraphMailProperties properties,
                            ObjectMapper objectMapper,
                            GraphMailSignatureService graphMailSignatureService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.graphMailSignatureService = graphMailSignatureService;
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public String buildAuthorizeUrl(String state) {
        return UriComponentsBuilder
                .fromUriString("https://login.microsoftonline.com/{tenant}/oauth2/v2.0/authorize")
                .buildAndExpand(properties.getTenantId())
                .toUriString()
                + "?"
                + formEncodedQuery(Map.of(
                "client_id", properties.getClientId(),
                "response_type", "code",
                "redirect_uri", properties.getRedirectUri(),
                "response_mode", "query",
                "scope", properties.getScopes(),
                "state", state,
                "prompt", "select_account"
        ));
    }

    public GraphTokenSession exchangeCode(String code) {
        GraphTokenSession session = tokenRequest(Map.of(
                "client_id", properties.getClientId(),
                "client_secret", properties.getClientSecret(),
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", properties.getRedirectUri(),
                "scope", properties.getScopes()
        ));
        populateAccountInfo(session);
        return session;
    }

    public GraphTokenSession refreshToken(String refreshToken) {
        GraphTokenSession session = tokenRequest(Map.of(
                "client_id", properties.getClientId(),
                "client_secret", properties.getClientSecret(),
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "redirect_uri", properties.getRedirectUri(),
                "scope", properties.getScopes()
        ));
        populateAccountInfo(session);
        return session;
    }

    public GraphTokenSession ensureValidToken(GraphTokenSession tokenSession) {
        if (tokenSession == null || tokenSession.getAccessToken() == null) {
            throw new IllegalStateException("Microsoft Graph is not connected.");
        }
        Instant expiresAt = tokenSession.getExpiresAt();
        if (expiresAt != null && expiresAt.isAfter(Instant.now().plusSeconds(60))) {
            return tokenSession;
        }
        if (tokenSession.getRefreshToken() == null || tokenSession.getRefreshToken().isBlank()) {
            throw new IllegalStateException("Microsoft Graph token expired and no refresh token is available.");
        }
        return refreshToken(tokenSession.getRefreshToken());
    }

    public void sendMail(String accessToken, String to, String cc, String subject, String body) {
        try {
            String htmlBody = graphMailSignatureService.buildHtmlBody(body);
            String payload = objectMapper.writeValueAsString(Map.of(
                    "message", Map.of(
                            "subject", subject,
                            "body", Map.of(
                                    "contentType", "HTML",
                                    "content", htmlBody
                            ),
                            "toRecipients", recipients(to),
                            "ccRecipients", recipients(cc),
                            "attachments", graphMailSignatureService.inlineAttachments().stream()
                                    .map(att -> Map.of(
                                            "@odata.type", "#microsoft.graph.fileAttachment",
                                            "name", att.name(),
                                            "contentType", att.contentType(),
                                            "isInline", true,
                                            "contentId", att.contentId(),
                                            "contentBytes", att.contentBytes()
                                    ))
                                    .toArray()
                    ),
                    "saveToSentItems", true
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://graph.microsoft.com/v1.0/me/sendMail"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Microsoft Graph sendMail failed: HTTP " + response.statusCode() + " - " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cannot send mail through Microsoft Graph.", e);
        }
    }

    private void populateAccountInfo(GraphTokenSession session) {
        if (session == null || session.getAccessToken() == null || session.getAccessToken().isBlank()) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://graph.microsoft.com/v1.0/me?$select=displayName,mail,userPrincipalName"))
                    .header("Authorization", "Bearer " + session.getAccessToken())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return;
            }
            JsonNode json = objectMapper.readTree(response.body());
            String mail = readText(json, "mail");
            String userPrincipalName = readText(json, "userPrincipalName");
            session.setAccountEmail(mail.isBlank() ? userPrincipalName : mail);
            session.setAccountDisplayName(readText(json, "displayName"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            // Mail sending still works even if account display metadata cannot be loaded.
        }
    }

    private Object[] recipients(String addresses) {
        if (addresses == null || addresses.isBlank()) {
            return new Object[0];
        }
        return java.util.Arrays.stream(addresses.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(address -> Map.of("emailAddress", Map.of("address", address)))
                .toArray();
    }

    private GraphTokenSession tokenRequest(Map<String, String> formValues) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://login.microsoftonline.com/" + properties.getTenantId() + "/oauth2/v2.0/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formEncodedQuery(formValues)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Microsoft token request failed: HTTP " + response.statusCode() + " - " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            GraphTokenSession session = new GraphTokenSession();
            session.setAccessToken(readText(json, "access_token"));
            session.setRefreshToken(readText(json, "refresh_token"));
            long expiresIn = json.path("expires_in").asLong(3600);
            session.setExpiresAt(Instant.now().plusSeconds(Math.max(0, expiresIn - 60)));
            return session;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cannot complete Microsoft token request.", e);
        }
    }

    private static String readText(JsonNode json, String field) {
        String value = json.path(field).asText();
        return value == null ? "" : value;
    }

    private static String formEncodedQuery(Map<String, String> values) {
        Map<String, String> ordered = new LinkedHashMap<>(values);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : ordered.entrySet()) {
            if (!first) sb.append("&");
            first = false;
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue() == null ? "" : entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
