package com.rebit.bamoe.service;

import com.rebit.bamoe.entity.LoanApplication;
import com.rebit.bamoe.entity.ApplicationStatus;
import com.rebit.bamoe.repo.LoanApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * Service for handling multi-level approval workflow decisions
 * Handles decisions from both Maker and Checker roles
 */
@Service
public class ApprovalService {

    @Autowired
    private LoanApplicationRepository loanRepository;

    @Autowired
    private NotificationService notificationService;

    // ============================================================
    // MAKER APPROVAL OPERATIONS
    // ============================================================

    /**
     * Maker approves the application
     */
    public void approveBrMaker(Long applicationId, String makerName, String makerEmail, String comments) {
        LoanApplication app = loanRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        app.setMakerName(makerName);
        app.setMakerEmail(makerEmail);
        app.setMakerDecision("APPROVED");
        app.setMakerComments(comments);
        app.setMakerReviewDate(LocalDateTime.now());
        app.setCurrentStage("CHECKER_REVIEW");
        app.setStatus(ApplicationStatus.MAKER_APPROVED);

        loanRepository.save(app);

        // Send notification to applicant that application moved to checker
        notificationService.notifyMakerApproval(app);
    }

    /**
     * Maker rejects the application
     */
    public void rejectByMaker(Long applicationId, String makerName, String makerEmail, String rejectReason) {
        LoanApplication app = loanRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        app.setMakerName(makerName);
        app.setMakerEmail(makerEmail);
        app.setMakerDecision("REJECTED");
        app.setMakerComments(rejectReason);
        app.setMakerReviewDate(LocalDateTime.now());
        app.setRejectionReason(rejectReason);
        app.setStatus(ApplicationStatus.MAKER_REJECTED);

        loanRepository.save(app);

        // Send rejection notification to applicant
        notificationService.notifyMakerRejection(app);
    }

    /**
     * Maker requests edit from applicant
     */
    public void requestEditByMaker(Long applicationId, String makerName, String makerEmail, String editReason) {
        LoanApplication app = loanRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        app.setMakerName(makerName);
        app.setMakerEmail(makerEmail);
        app.setMakerDecision("EDIT");
        app.setEditReason(editReason);
        app.setMakerComments("Please make the requested changes and resubmit your application.");
        app.setMakerReviewDate(LocalDateTime.now());
        app.setCurrentStage("EDIT_REQUESTED");
        app.setStatus(ApplicationStatus.EDIT_REQUESTED);
        app.setEditCount(app.getEditCount() + 1);

        loanRepository.save(app);

        // Send edit request to applicant
        notificationService.notifyEditRequest(app);
    }

    // ============================================================
    // CHECKER APPROVAL OPERATIONS
    // ============================================================

    /**
     * Checker approves the application (Final approval)
     */
    public void approveByChecker(Long applicationId, String checkerName, String checkerEmail, String comments) {
        LoanApplication app = loanRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        app.setCheckerName(checkerName);
        app.setCheckerEmail(checkerEmail);
        app.setCheckerDecision("APPROVED");
        app.setCheckerComments(comments);
        app.setCheckerReviewDate(LocalDateTime.now());
        app.setStatus(ApplicationStatus.APPROVED);

        loanRepository.save(app);

        // Send approval notification to applicant
        notificationService.notifyApproval(app);
    }

    /**
     * Checker rejects the application
     */
    public void rejectByChecker(Long applicationId, String checkerName, String checkerEmail, String rejectReason) {
        LoanApplication app = loanRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        app.setCheckerName(checkerName);
        app.setCheckerEmail(checkerEmail);
        app.setCheckerDecision("REJECTED");
        app.setCheckerComments(rejectReason);
        app.setCheckerReviewDate(LocalDateTime.now());
        app.setRejectionReason(rejectReason);
        app.setStatus(ApplicationStatus.REJECTED);

        loanRepository.save(app);

        // Send rejection notification to applicant and maker
        notificationService.notifyRejection(app);
    }

    /**
     * Checker sends application back to maker for revision
     */
    public void sendBackToMaker(Long applicationId, String checkerName, String checkerEmail, String reason) {
        LoanApplication app = loanRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        app.setCheckerName(checkerName);
        app.setCheckerEmail(checkerEmail);
        app.setCheckerDecision("SEND_BACK");
        app.setCheckerComments(reason);
        app.setCheckerReviewDate(LocalDateTime.now());
        app.setCurrentStage("MAKER_REVIEW");
        app.setStatus(ApplicationStatus.MAKER_REVIEW);

        loanRepository.save(app);

        // Notify maker to review again
        notificationService.notifySendBackToMaker(app);
    }

    // ============================================================
    // EDIT OPERATIONS
    // ============================================================

    /**
     * User edits and resubmits application
     */
    public void resubmitAfterEdit(Long applicationId, LoanApplication updatedApp) {
        LoanApplication app = loanRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        // Update applicant information
        app.setApplicantName(updatedApp.getApplicantName());
        app.setApplicantEmail(updatedApp.getApplicantEmail());
        app.setApplicantPhone(updatedApp.getApplicantPhone());
        app.setLoanAmount(updatedApp.getLoanAmount());
        app.setLoanTerm(updatedApp.getLoanTerm());
        app.setAnnualIncome(updatedApp.getAnnualIncome());
        app.setCreditScore(updatedApp.getCreditScore());
        app.setEmploymentStatus(updatedApp.getEmploymentStatus());
        app.setTermsAccepted(updatedApp.isTermsAccepted());

        // Reset workflow state
        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setCurrentStage("MAKER_REVIEW");
        app.setMakerDecision(null);
        app.setMakerComments(null);

        loanRepository.save(app);

        // Notify maker that application has been resubmitted
        notificationService.notifyResubmission(app);
    }

    // ============================================================
    // QUERY OPERATIONS
    // ============================================================

    /**
     * Get application by ID with all details
     */
    public LoanApplication getApplicationDetails(Long applicationId) {
        return loanRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));
    }

    /**
     * Check if application is in maker review stage
     */
    public boolean isInMakerReview(Long applicationId) {
        LoanApplication app = getApplicationDetails(applicationId);
        return "MAKER_REVIEW".equals(app.getCurrentStage());
    }

    /**
     * Check if application is in checker review stage
     */
    public boolean isInCheckerReview(Long applicationId) {
        LoanApplication app = getApplicationDetails(applicationId);
        return "CHECKER_REVIEW".equals(app.getCurrentStage());
    }

    /**
     * Check if application is awaiting user edit
     */
    public boolean isEditRequested(Long applicationId) {
        LoanApplication app = getApplicationDetails(applicationId);
        return "EDIT_REQUESTED".equals(app.getCurrentStage());
    }

    /**
     * Get application audit trail
     */
    public String getAuditTrail(Long applicationId) {
        LoanApplication app = getApplicationDetails(applicationId);
        StringBuilder audit = new StringBuilder();

        audit.append("=== APPLICATION AUDIT TRAIL ===\n");
        audit.append(String.format("Application ID: %d\n", app.getId()));
        audit.append(String.format("Applicant: %s (%s)\n", app.getApplicantName(), app.getApplicantEmail()));
        audit.append(String.format("Current Status: %s\n", app.getStatus().getDisplayName()));
        audit.append(String.format("Current Stage: %s\n", app.getCurrentStage()));
        audit.append(String.format("Loan Amount: %s\n", app.getLoanAmount()));
        audit.append(String.format("Credit Score: %d\n", app.getCreditScore()));
        audit.append(String.format("Edit Count: %d\n", app.getEditCount()));

        if (app.getMakerReviewDate() != null) {
            audit.append(String.format("\nMAKER REVIEW:\n"));
            audit.append(String.format("  Reviewer: %s (%s)\n", app.getMakerName(), app.getMakerEmail()));
            audit.append(String.format("  Decision: %s\n", app.getMakerDecision()));
            audit.append(String.format("  Date: %s\n", app.getMakerReviewDate()));
            audit.append(String.format("  Comments: %s\n", app.getMakerComments()));
        }

        if (app.getCheckerReviewDate() != null) {
            audit.append(String.format("\nCHECKER REVIEW:\n"));
            audit.append(String.format("  Reviewer: %s (%s)\n", app.getCheckerName(), app.getCheckerEmail()));
            audit.append(String.format("  Decision: %s\n", app.getCheckerDecision()));
            audit.append(String.format("  Date: %s\n", app.getCheckerReviewDate()));
            audit.append(String.format("  Comments: %s\n", app.getCheckerComments()));
        }

        if (app.getEditReason() != null) {
            audit.append(String.format("\nEDIT REQUEST:\n"));
            audit.append(String.format("  Reason: %s\n", app.getEditReason()));
        }

        return audit.toString();
    }
}