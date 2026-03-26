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

import java.util.UUID;

@Controller
@RequestMapping("/microsoft/graph")
public class GraphMailController {

    public static final String SESSION_STATE_KEY = "graph.oauth.state";
    public static final String SESSION_TOKEN_KEY = "graph.oauth.token";
    public static final String SESSION_PENDING_DRAFT_KEY = "graph.pending.mail";
    public static final String SESSION_RETURN_TO_KEY = "graph.return.to";

    private final GraphMailService graphMailService;

    public GraphMailController(GraphMailService graphMailService) {
        this.graphMailService = graphMailService;
    }

    @GetMapping("/connect")
    public RedirectView connect(@RequestParam(required = false) String returnTo,
                                HttpSession session) {
        String state = UUID.randomUUID().toString();
        session.setAttribute(SESSION_STATE_KEY, state);
        session.setAttribute(SESSION_RETURN_TO_KEY, safeReturnTo(returnTo));
        return new RedirectView(graphMailService.buildAuthorizeUrl(state));
    }

    @GetMapping("/callback")
    public RedirectView callback(@RequestParam String code,
                                 @RequestParam String state,
                                 HttpSession session) {
        String expectedState = (String) session.getAttribute(SESSION_STATE_KEY);
        String returnTo = safeReturnTo((String) session.getAttribute(SESSION_RETURN_TO_KEY));

        session.removeAttribute(SESSION_STATE_KEY);
        session.removeAttribute(SESSION_RETURN_TO_KEY);

        if (expectedState == null || !expectedState.equals(state)) {
            return new RedirectView(returnTo + separator(returnTo) + "mailStatus=graphStateInvalid");
        }

        try {
            GraphTokenSession tokenSession = graphMailService.exchangeCode(code);
            session.setAttribute(SESSION_TOKEN_KEY, tokenSession);

            GraphMailDraft pendingDraft = (GraphMailDraft) session.getAttribute(SESSION_PENDING_DRAFT_KEY);
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

    private static String separator(String path) {
        return path.contains("?") ? "&" : "?";
    }
}
