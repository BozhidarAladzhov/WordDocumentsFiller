package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.dto.GraphMailDraft;
import com.example.WordDocumentsFiller.dto.GraphTokenSession;
import com.example.WordDocumentsFiller.service.GraphMailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Controller
@RequestMapping("/microsoft/graph")
public class GraphMailController {

    public static final String SESSION_STATE_KEY = "graph.oauth.state";
    public static final String SESSION_TOKEN_KEY = "graph.oauth.token";
    public static final String SESSION_PENDING_DRAFT_KEY = "graph.pending.mail";
    public static final String SESSION_RETURN_TO_KEY = "graph.return.to";
    public static final String SESSION_SEND_PENDING_KEY = "graph.send.pending";

    private final GraphMailService graphMailService;

    public GraphMailController(GraphMailService graphMailService) {
        this.graphMailService = graphMailService;
    }

    @GetMapping("/connect")
    public RedirectView connect(@RequestParam(required = false) String returnTo,
                                @RequestParam(defaultValue = "true") boolean sendPending,
                                HttpSession session) {
        String safeReturnTo = safeReturnTo(returnTo);
        String state = buildState(safeReturnTo, sendPending);
        session.setAttribute(SESSION_STATE_KEY, state);
        session.setAttribute(SESSION_RETURN_TO_KEY, safeReturnTo);
        session.setAttribute(SESSION_SEND_PENDING_KEY, sendPending);
        if (!sendPending) {
            session.removeAttribute(SESSION_PENDING_DRAFT_KEY);
        }
        return new RedirectView(graphMailService.buildAuthorizeUrl(state));
    }

    @GetMapping("/callback")
    public RedirectView callback(@RequestParam String code,
                                 @RequestParam String state,
                                 HttpSession session) {
        ParsedState parsedState = parseState(state);
        String expectedState = (String) session.getAttribute(SESSION_STATE_KEY);
        String sessionReturnTo = (String) session.getAttribute(SESSION_RETURN_TO_KEY);
        String returnTo = safeReturnTo(sessionReturnTo != null ? sessionReturnTo :
                (parsedState == null ? null : parsedState.returnTo()));

        session.removeAttribute(SESSION_STATE_KEY);
        session.removeAttribute(SESSION_RETURN_TO_KEY);
        Boolean sendPending = (Boolean) session.getAttribute(SESSION_SEND_PENDING_KEY);
        session.removeAttribute(SESSION_SEND_PENDING_KEY);
        if (sendPending == null && parsedState != null) {
            sendPending = parsedState.sendPending();
        }

        if (parsedState == null || (expectedState != null && !expectedState.equals(state))) {
            return new RedirectView(returnTo + separator(returnTo) + "mailStatus=graphStateInvalid");
        }

        try {
            GraphTokenSession tokenSession = graphMailService.exchangeCode(code);
            session.setAttribute(SESSION_TOKEN_KEY, tokenSession);

            GraphMailDraft pendingDraft = Boolean.FALSE.equals(sendPending)
                    ? null
                    : (GraphMailDraft) session.getAttribute(SESSION_PENDING_DRAFT_KEY);
            if (pendingDraft != null) {
                session.removeAttribute(SESSION_PENDING_DRAFT_KEY);
                GraphTokenSession validToken = graphMailService.ensureValidToken(tokenSession);
                session.setAttribute(SESSION_TOKEN_KEY, validToken);
                graphMailService.sendMail(validToken.getAccessToken(), pendingDraft.to(), pendingDraft.cc(), pendingDraft.subject(), pendingDraft.body());
                return new RedirectView(pendingDraft.returnTo() + separator(pendingDraft.returnTo()) + "mailStatus=sent");
            }

            return new RedirectView(returnTo + separator(returnTo) + "mailStatus=connected");
        } catch (Exception ex) {
            return new RedirectView(returnTo + separator(returnTo) + "mailStatus=graphError");
        }
    }

    static String safeReturnTo(String returnTo) {
        if (returnTo == null || returnTo.isBlank() || !returnTo.startsWith("/")) {
            return "/container-tracker/containers";
        }
        return returnTo;
    }

    static String buildState(String returnTo, boolean sendPending) {
        String encodedReturnTo = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(safeReturnTo(returnTo).getBytes(StandardCharsets.UTF_8));
        return UUID.randomUUID() + "." + encodedReturnTo + "." + (sendPending ? "1" : "0");
    }

    static ParsedState parseState(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }
        String[] parts = state.split("\\.", 3);
        if (parts.length != 3) {
            return null;
        }
        try {
            UUID.fromString(parts[0]);
            String returnTo = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            boolean sendPending = "1".equals(parts[2]);
            return new ParsedState(safeReturnTo(returnTo), sendPending);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String separator(String path) {
        return path.contains("?") ? "&" : "?";
    }

    record ParsedState(String returnTo, boolean sendPending) {
    }
}
