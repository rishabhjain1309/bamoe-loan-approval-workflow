package com.rebit.bamoe.controller;

import com.rebit.bamoe.entity.LoanApplication;
import com.rebit.bamoe.repo.LoanApplicationRepository;
import com.rebit.bamoe.service.ApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for maker and checker decisions.
 *
 * All endpoints call ApprovalService which:
 *   1. Updates the LoanApplication in MySQL
 *   2. Completes the Kogito user task (advancing the BPMN)
 *
 * Base path: /api/approval  (because server.servlet.context-path=/api)
 */
@RestController
@RequestMapping("/approval")
@CrossOrigin(origins = "*")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private LoanApplicationRepository loanRepository;

    // =====================================================================
    // MAKER ENDPOINTS
    // =====================================================================

    /**
     * List all applications awaiting maker review.
     * Optionally filter by the logged-in maker's username.
     *
     * GET /api/approval/maker/pending?assignee=maker1
     */
    @GetMapping("/maker/pending")
    public ResponseEntity<List<LoanApplication>> getMakerPending(
            @RequestParam(required = false) String assignee) {

        List<LoanApplication> pending;
        if (assignee != null && !assignee.isBlank()) {
            pending = loanRepository.findByCurrentStageAndMakerAssignee("MAKER_REVIEW", assignee);
        } else {
            pending = loanRepository.findByCurrentStage("MAKER_REVIEW");
        }
        return ResponseEntity.ok(pending);
    }

    /**
     * Get a single application for maker review.
     * GET /api/approval/maker/review/{id}
     */
    @GetMapping("/maker/review/{id}")
    public ResponseEntity<?> getMakerReviewDetails(@PathVariable Long id) {
        if (!approvalService.isInMakerReview(id)) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Application " + id + " is not in MAKER_REVIEW stage"));
        }
        return ResponseEntity.ok(approvalService.getApplicationDetails(id));
    }

    /**
     * Maker approves. Advances BPMN to Checker Review.
     * POST /api/approval/maker/{id}/approve
     * Params: makerName, makerEmail, comments (optional)
     */
    @PostMapping("/maker/{id}/approve")
    public ResponseEntity<Map<String, String>> approveByMaker(
            @PathVariable Long id,
            @RequestParam String makerName,
            @RequestParam String makerEmail,
            @RequestParam(required = false) String comments) {

        approvalService.approveByMaker(id, makerName, makerEmail, comments);
        return ResponseEntity.ok(Map.of(
                "message", "Application approved by maker. Now in Checker Review.",
                "applicationId", id.toString()
        ));
    }

    /**
     * Maker rejects. BPMN ends at EndRejectedByMaker.
     * POST /api/approval/maker/{id}/reject
     * Params: makerName, makerEmail, rejectReason
     */
    @PostMapping("/maker/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectByMaker(
            @PathVariable Long id,
            @RequestParam String makerName,
            @RequestParam String makerEmail,
            @RequestParam String rejectReason) {

        approvalService.rejectByMaker(id, makerName, makerEmail, rejectReason);
        return ResponseEntity.ok(Map.of(
                "message", "Application rejected by maker.",
                "applicationId", id.toString()
        ));
    }

    /**
     * Maker requests edit. BPMN loops back to MakerReview after user resubmits.
     * POST /api/approval/maker/{id}/request-edit
     * Params: makerName, makerEmail, editReason
     */
    @PostMapping("/maker/{id}/request-edit")
    public ResponseEntity<Map<String, String>> requestEdit(
            @PathVariable Long id,
            @RequestParam String makerName,
            @RequestParam String makerEmail,
            @RequestParam String editReason) {

        approvalService.requestEditByMaker(id, makerName, makerEmail, editReason);
        return ResponseEntity.ok(Map.of(
                "message", "Edit request sent to applicant.",
                "applicationId", id.toString()
        ));
    }

    // =====================================================================
    // CHECKER ENDPOINTS
    // =====================================================================

    /**
     * List all applications awaiting checker review.
     * Optionally filter by the logged-in checker's username.
     *
     * GET /api/approval/checker/pending?assignee=checker1
     */
    @GetMapping("/checker/pending")
    public ResponseEntity<List<LoanApplication>> getCheckerPending(
            @RequestParam(required = false) String assignee) {

        List<LoanApplication> pending;
        if (assignee != null && !assignee.isBlank()) {
            pending = loanRepository.findByCurrentStageAndCheckerAssignee("CHECKER_REVIEW", assignee);
        } else {
            pending = loanRepository.findByCurrentStage("CHECKER_REVIEW");
        }
        return ResponseEntity.ok(pending);
    }

    /**
     * Get a single application for checker review.
     * GET /api/approval/checker/review/{id}
     */
    @GetMapping("/checker/review/{id}")
    public ResponseEntity<?> getCheckerReviewDetails(@PathVariable Long id) {
        if (!approvalService.isInCheckerReview(id)) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Application " + id + " is not in CHECKER_REVIEW stage"));
        }
        return ResponseEntity.ok(approvalService.getApplicationDetails(id));
    }

    /**
     * Checker approves (final approval). BPMN ends at EndApproved.
     * POST /api/approval/checker/{id}/approve
     */
    @PostMapping("/checker/{id}/approve")
    public ResponseEntity<Map<String, String>> approveByChecker(
            @PathVariable Long id,
            @RequestParam String checkerName,
            @RequestParam String checkerEmail,
            @RequestParam(required = false) String comments) {

        approvalService.approveByChecker(id, checkerName, checkerEmail, comments);
        return ResponseEntity.ok(Map.of(
                "message", "Application APPROVED.",
                "applicationId", id.toString()
        ));
    }

    /**
     * Checker rejects. BPMN ends at EndRejectedByChecker.
     * POST /api/approval/checker/{id}/reject
     */
    @PostMapping("/checker/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectByChecker(
            @PathVariable Long id,
            @RequestParam String checkerName,
            @RequestParam String checkerEmail,
            @RequestParam String rejectReason) {

        approvalService.rejectByChecker(id, checkerName, checkerEmail, rejectReason);
        return ResponseEntity.ok(Map.of(
                "message", "Application rejected by checker.",
                "applicationId", id.toString()
        ));
    }

    /**
     * Checker sends back to maker. BPMN loops back to MakerReview.
     * POST /api/approval/checker/{id}/send-back
     */
    @PostMapping("/checker/{id}/send-back")
    public ResponseEntity<Map<String, String>> sendBackToMaker(
            @PathVariable Long id,
            @RequestParam String checkerName,
            @RequestParam String checkerEmail,
            @RequestParam String reason) {

        approvalService.sendBackToMaker(id, checkerName, checkerEmail, reason);
        return ResponseEntity.ok(Map.of(
                "message", "Application sent back to maker for review.",
                "applicationId", id.toString()
        ));
    }

    // =====================================================================
    // APPLICANT RESUBMIT
    // =====================================================================

    /**
     * Applicant edits and resubmits after maker requested edit.
     * PUT /api/approval/applications/{id}/resubmit
     */
    @PutMapping("/applications/{id}/resubmit")
    public ResponseEntity<?> resubmitApplication(
            @PathVariable Long id,
            @RequestBody LoanApplication updatedApp) {

        if (!approvalService.isEditRequested(id)) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Application is not in EDIT_REQUESTED state"));
        }
        approvalService.resubmitAfterEdit(id, updatedApp);
        return ResponseEntity.ok(Map.of(
                "message", "Application resubmitted. Awaiting maker review.",
                "applicationId", id.toString()
        ));
    }

    // =====================================================================
    // STATUS + AUDIT
    // =====================================================================

    /**
     * GET /api/approval/applications/{id}/status
     */
    @GetMapping("/applications/{id}/status")
    public ResponseEntity<LoanApplication> getApplicationStatus(@PathVariable Long id) {
        return ResponseEntity.ok(approvalService.getApplicationDetails(id));
    }

    /**
     * GET /api/approval/applications/{id}/audit
     */
    @GetMapping("/applications/{id}/audit")
    public ResponseEntity<String> getAuditTrail(@PathVariable Long id) {
        return ResponseEntity.ok(approvalService.getAuditTrail(id));
    }
}