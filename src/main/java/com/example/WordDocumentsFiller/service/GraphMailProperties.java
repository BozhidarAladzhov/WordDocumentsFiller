package com.example.WordDocumentsFiller.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "microsoft.graph")
public class GraphMailProperties {

    private boolean enabled;
    private String tenantId;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scopes = "openid offline_access User.Read Mail.Send";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public boolean isConfigured() {
        return enabled
                && hasText(tenantId)
                && hasText(clientId)
                && hasText(clientSecret)
                && hasText(redirectUri);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
