package com.rebit.bamoe.controller;

import com.rebit.bamoe.entity.LoanApplication;
import com.rebit.bamoe.service.ApprovalService;
import com.rebit.bamoe.repo.LoanApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/approval")
@CrossOrigin(origins = "*")
public class ApprovalController {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private LoanApplicationRepository loanRepository;

    // ============================================================
    // MAKER ENDPOINTS
    // ============================================================

    /**
     * Get all applications pending maker review
     * GET /api/approval/maker/pending
     */
    @GetMapping("/maker/pending")
    public ResponseEntity<List<LoanApplication>> getMakerPending() {
        List<LoanApplication> pending = loanRepository.findAll().stream()
                .filter(app -> "MAKER_REVIEW".equals(app.getCurrentStage()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(pending);
    }

    /**
     * Get single application for maker review
     * GET /api/approval/maker/review/{id}
     */
    @GetMapping("/maker/review/{id}")
    public ResponseEntity<LoanApplication> getMakerReviewDetails(@PathVariable Long id) {
        LoanApplication app = approvalService.getApplicationDetails(id);
        if (!approvalService.isInMakerReview(id)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(app);
    }

    /**
     * Maker approves application
     * POST /api/approval/maker/{id}/approve
     */
    @PostMapping("/maker/{id}/approve")
    public ResponseEntity<Map<String, String>> approveBrMaker(
            @PathVariable Long id,
            @RequestParam String makerName,
            @RequestParam String makerEmail,
            @RequestParam(required = false) String comments) {

        try {
            approvalService.approveBrMaker(id, makerName, makerEmail, comments);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Application approved by maker");
            response.put("applicationId", id.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Maker rejects application
     * POST /api/approval/maker/{id}/reject
     */
    @PostMapping("/maker/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectByMaker(
            @PathVariable Long id,
            @RequestParam String makerName,
            @RequestParam String makerEmail,
            @RequestParam String rejectReason) {

        try {
            approvalService.rejectByMaker(id, makerName, makerEmail, rejectReason);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Application rejected by maker");
            response.put("applicationId", id.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Maker requests edit from applicant
     * POST /api/approval/maker/{id}/request-edit
     */
    @PostMapping("/maker/{id}/request-edit")
    public ResponseEntity<Map<String, String>> requestEdit(
            @PathVariable Long id,
            @RequestParam String makerName,
            @RequestParam String makerEmail,
            @RequestParam String editReason) {

        try {
            approvalService.requestEditByMaker(id, makerName, makerEmail, editReason);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Edit request sent to applicant");
            response.put("applicationId", id.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ============================================================
    // CHECKER ENDPOINTS
    // ============================================================

    /**
     * Get all applications pending checker review
     * GET /api/approval/checker/pending
     */
    @GetMapping("/checker/pending")
    public ResponseEntity<List<LoanApplication>> getCheckerPending() {
        List<LoanApplication> pending = loanRepository.findAll().stream()
                .filter(app -> "CHECKER_REVIEW".equals(app.getCurrentStage()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(pending);
    }

    /**
     * Get single application for checker review
     * GET /api/approval/checker/review/{id}
     */
    @GetMapping("/checker/review/{id}")
    public ResponseEntity<LoanApplication> getCheckerReviewDetails(@PathVariable Long id) {
        LoanApplication app = approvalService.getApplicationDetails(id);
        if (!approvalService.isInCheckerReview(id)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(app);
    }

    /**
     * Checker approves application (Final)
     * POST /api/approval/checker/{id}/approve
     */
    @PostMapping("/checker/{id}/approve")
    public ResponseEntity<Map<String, String>> approveByChecker(
            @PathVariable Long id,
            @RequestParam String checkerName,
            @RequestParam String checkerEmail,
            @RequestParam(required = false) String comments) {

        try {
            approvalService.approveByChecker(id, checkerName, checkerEmail, comments);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Application approved by checker");
            response.put("applicationId", id.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Checker rejects application
     * POST /api/approval/checker/{id}/reject
     */
    @PostMapping("/checker/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectByChecker(
            @PathVariable Long id,
            @RequestParam String checkerName,
            @RequestParam String checkerEmail,
            @RequestParam String rejectReason) {

        try {
            approvalService.rejectByChecker(id, checkerName, checkerEmail, rejectReason);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Application rejected by checker");
            response.put("applicationId", id.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Checker sends application back to maker
     * POST /api/approval/checker/{id}/send-back
     */
    @PostMapping("/checker/{id}/send-back")
    public ResponseEntity<Map<String, String>> sendBackToMaker(
            @PathVariable Long id,
            @RequestParam String checkerName,
            @RequestParam String checkerEmail,
            @RequestParam String reason) {

        try {
            approvalService.sendBackToMaker(id, checkerName, checkerEmail, reason);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Application sent back to maker");
            response.put("applicationId", id.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ============================================================
    // APPLICANT ENDPOINTS
    // ============================================================

    /**
     * User resubmits application after edit request
     * PUT /api/approval/applications/{id}/resubmit
     */
    @PutMapping("/applications/{id}/resubmit")
    public ResponseEntity<Map<String, String>> resubmitApplication(
            @PathVariable Long id,
            @RequestBody LoanApplication updatedApp) {

        try {
            if (!approvalService.isEditRequested(id)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Application is not in edit requested state")
                );
            }

            approvalService.resubmitAfterEdit(id, updatedApp);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Application resubmitted successfully");
            response.put("applicationId", id.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ============================================================
    // AUDIT ENDPOINTS
    // ============================================================

    /**
     * Get complete audit trail of application
     * GET /api/approval/applications/{id}/audit
     */
    @GetMapping("/applications/{id}/audit")
    public ResponseEntity<String> getAuditTrail(@PathVariable Long id) {
        try {
            String audit = approvalService.getAuditTrail(id);
            return ResponseEntity.ok(audit);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get application status
     * GET /api/approval/applications/{id}/status
     */
    @GetMapping("/applications/{id}/status")
    public ResponseEntity<LoanApplication> getApplicationStatus(@PathVariable Long id) {
        try {
            LoanApplication app = approvalService.getApplicationDetails(id);
            return ResponseEntity.ok(app);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}