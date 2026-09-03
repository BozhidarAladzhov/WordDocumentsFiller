package com.example.WordDocumentsFiller.controllers;

import com.example.WordDocumentsFiller.dto.GraphMailDraft;
import com.example.WordDocumentsFiller.dto.GraphTokenSession;
import com.example.WordDocumentsFiller.dto.OverdueDebtMailDraft;
import com.example.WordDocumentsFiller.service.GraphMailProperties;
import com.example.WordDocumentsFiller.service.GraphMailService;
import com.example.WordDocumentsFiller.service.OverdueDebtsService;
import com.example.WordDocumentsFiller.service.ProcreditProcessingException;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/overdue-debts")
public class OverdueDebtsController {

    private static final String SESSION_DRAFTS_KEY = "overdue.debts.drafts";
    private static final String SESSION_INSTANCE_KEY = "overdue.debts.instance";
    private static final String SESSION_PRESERVE_ONCE_KEY = "overdue.debts.preserve.once";
    private static final String RETURN_TO = "/overdue-debts";

    private final OverdueDebtsService overdueDebtsService;
    private final GraphMailService graphMailService;
    private final GraphMailProperties graphMailProperties;
    private final String instanceId = UUID.randomUUID().toString();

    public OverdueDebtsController(OverdueDebtsService overdueDebtsService,
                                  GraphMailService graphMailService,
                                  GraphMailProperties graphMailProperties) {
        this.overdueDebtsService = overdueDebtsService;
        this.graphMailService = graphMailService;
        this.graphMailProperties = graphMailProperties;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String mailStatus,
                        HttpSession session,
                        Model model) {
        if (!consumePreserveOnce(session)) {
            clearDrafts(session);
        }
        addPageModel(model, session, mailStatus, null);
        return "overdue-debts";
    }

    @GetMapping("/connect-outlook")
    public String connectOutlook(HttpSession session) {
        session.setAttribute(SESSION_PRESERVE_ONCE_KEY, true);
        return "redirect:/microsoft/graph/connect?returnTo=" + urlEncode(RETURN_TO) + "&sendPending=false";
    }

    @PostMapping("/preview")
    public String preview(@RequestParam("file") MultipartFile file,
                          HttpSession session,
                          Model model) {
        try {
            List<OverdueDebtMailDraft> drafts = overdueDebtsService.buildDrafts(file, LocalDate.now());
            session.setAttribute(SESSION_DRAFTS_KEY, new ArrayList<>(drafts));
            session.setAttribute(SESSION_INSTANCE_KEY, instanceId);
            addPageModel(model, session, null, null);
        } catch (ProcreditProcessingException ex) {
            clearDrafts(session);
            addPageModel(model, session, null, ex.getMessage());
        } catch (IOException ex) {
            clearDrafts(session);
            addPageModel(model, session, null, "Файлът не може да бъде прочетен като Excel.");
        }
        return "overdue-debts";
    }

    @PostMapping("/drafts/{draftId}/send")
    public Object send(@PathVariable String draftId,
                       @RequestParam String to,
                       @RequestParam(required = false) String cc,
                       @RequestParam String subject,
                       @RequestParam String body,
                       @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                       HttpSession session) {
        boolean ajax = "XMLHttpRequest".equalsIgnoreCase(requestedWith);
        OverdueDebtMailDraft draft = findDraft(session, draftId);
        if (draft == null) {
            if (ajax) {
                return ResponseEntity.badRequest().body(Map.of("status", "draftMissing", "message", "Draft-ът вече не е наличен."));
            }
            return "redirect:" + RETURN_TO + "?mailStatus=draftMissing";
        }
        if (safeText(to).isBlank()) {
            if (ajax) {
                return ResponseEntity.badRequest().body(Map.of("status", "missingRecipient", "message", "Липсва получател."));
            }
            return "redirect:" + RETURN_TO + "?mailStatus=missingRecipient";
        }
        if (!graphMailProperties.isConfigured()) {
            if (ajax) {
                return ResponseEntity.badRequest().body(Map.of("status", "graphNotConfigured", "message", "Microsoft Graph не е настроен."));
            }
            return "redirect:" + RETURN_TO + "?mailStatus=graphNotConfigured";
        }

        String ccForSend = safeText(cc);
        try {
            GraphTokenSession token = (GraphTokenSession) session.getAttribute(GraphMailController.SESSION_TOKEN_KEY);
            if (token == null) {
                if (ajax) {
                    return ResponseEntity.status(401).body(Map.of("status", "notConnected", "message", "Първо свържи Outlook от бутона най-горе."));
                }
                return "redirect:" + RETURN_TO + "?mailStatus=notConnected";
            }

            GraphTokenSession validToken = graphMailService.ensureValidToken(token);
            session.setAttribute(GraphMailController.SESSION_TOKEN_KEY, validToken);
            graphMailService.sendMail(validToken.getAccessToken(), validToken.getAccountEmail(), to, ccForSend, subject, body);
            if (ajax) {
                return ResponseEntity.ok(Map.of("status", "sent", "message", "Имейлът е изпратен успешно."));
            }
            return "redirect:" + RETURN_TO + "?mailStatus=sent";
        } catch (Exception ex) {
            session.removeAttribute(GraphMailController.SESSION_TOKEN_KEY);
            session.setAttribute(GraphMailController.SESSION_PENDING_DRAFT_KEY,
                    new GraphMailDraft(to, ccForSend, subject, body, RETURN_TO));
            if (ajax) {
                return ResponseEntity.status(401).body(Map.of("status", "graphError", "message", "Outlook връзката не успя. Свържи Outlook отново от бутона най-горе."));
            }
            return "redirect:/microsoft/graph/connect?returnTo=" + urlEncode(RETURN_TO);
        }
    }

    private void addPageModel(Model model, HttpSession session, String mailStatus, String error) {
        clearStaleDrafts(session);
        List<OverdueDebtMailDraft> drafts = getDrafts(session);
        Map<Boolean, List<OverdueDebtMailDraft>> partitioned = drafts.stream()
                .collect(Collectors.partitioningBy(draft -> !safeText(draft.to()).isBlank()));
        List<OverdueDebtMailDraft> sendableDrafts = partitioned.get(true);
        List<OverdueDebtMailDraft> reviewDrafts = partitioned.get(false);

        model.addAttribute("drafts", drafts);
        model.addAttribute("draftCount", drafts.size());
        model.addAttribute("sendableDrafts", sendableDrafts);
        model.addAttribute("sendableDraftCount", sendableDrafts.size());
        model.addAttribute("reviewDrafts", reviewDrafts);
        model.addAttribute("reviewDraftCount", reviewDrafts.size());
        model.addAttribute("graphConfigured", graphMailProperties.isConfigured());
        GraphTokenSession token = (GraphTokenSession) session.getAttribute(GraphMailController.SESSION_TOKEN_KEY);
        model.addAttribute("graphConnected", token != null);
        model.addAttribute("graphAccountEmail", token == null ? "" : safeText(token.getAccountEmail()));
        model.addAttribute("graphAccountName", token == null ? "" : safeText(token.getAccountDisplayName()));
        model.addAttribute("mailStatus", mailStatus);
        model.addAttribute("error", error);
        model.addAttribute("today", LocalDate.now());
    }

    @SuppressWarnings("unchecked")
    private List<OverdueDebtMailDraft> getDrafts(HttpSession session) {
        Object value = session.getAttribute(SESSION_DRAFTS_KEY);
        if (value instanceof List<?>) {
            return (List<OverdueDebtMailDraft>) value;
        }
        return List.of();
    }

    private void clearStaleDrafts(HttpSession session) {
        Object draftInstance = session.getAttribute(SESSION_INSTANCE_KEY);
        if (draftInstance != null && !instanceId.equals(draftInstance)) {
            clearDrafts(session);
        }
    }

    private boolean consumePreserveOnce(HttpSession session) {
        Object preserve = session.getAttribute(SESSION_PRESERVE_ONCE_KEY);
        session.removeAttribute(SESSION_PRESERVE_ONCE_KEY);
        return Boolean.TRUE.equals(preserve);
    }

    private void clearDrafts(HttpSession session) {
        session.removeAttribute(SESSION_DRAFTS_KEY);
        session.removeAttribute(SESSION_INSTANCE_KEY);
    }

    private OverdueDebtMailDraft findDraft(HttpSession session, String draftId) {
        return getDrafts(session).stream()
                .filter(draft -> draft.id().equals(draftId))
                .findFirst()
                .orElse(null);
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
