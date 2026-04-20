package com.rebit.bamoe.service;

import com.rebit.bamoe.entity.ApplicationStatus;
import com.rebit.bamoe.entity.LoanApplication;
import com.rebit.bamoe.repo.LoanApplicationRepository;
import org.kie.kogito.Model;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.WorkItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles maker/checker decisions.
 *
 * Every decision method does TWO things in this order:
 *   1. Update the LoanApplication entity in MySQL (for your business records)
 *   2. Complete the Kogito user task (so the BPMN engine advances to the next step)
 *
 * NOTE: We inject the concrete generated Process bean directly by name
 * instead of using the Processes registry, which avoids the scan issue.
 */
@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    @Autowired
    private LoanApplicationRepository loanRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Inject the generated LoanApprovalProcessProcess bean directly.
     * Kogito generates a Spring bean named "loanApprovalProcess" (camelCase of the process id).
     * Using the interface type Process<Model> lets Spring resolve it without ambiguity.
     */
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("LoanApprovalProcess")
    private Process<? extends Model> loanApprovalProcess;

    // =====================================================================
    // MAKER DECISIONS
    // =====================================================================

    public void approveByMaker(Long applicationId, String makerName, String makerEmail, String comments) {
        LoanApplication app = findOrThrow(applicationId);

        app.setMakerName(makerName);
        app.setMakerEmail(makerEmail);
        app.setMakerDecision("APPROVE");
        app.setMakerComments(comments);
        app.setMakerReviewDate(LocalDateTime.now());
        app.moveToCheckerReview();
        loanRepository.save(app);

        completeKogitoTask(app, "MakerReview", Map.of(
                "makerDecision", "APPROVE",
                "comments", comments != null ? comments : ""
        ));

        notificationService.notifyMakerApproval(app);
    }

    public void rejectByMaker(Long applicationId, String makerName, String makerEmail, String rejectReason) {
        LoanApplication app = findOrThrow(applicationId);

        app.setMakerName(makerName);
        app.setMakerEmail(makerEmail);
        app.rejectByMaker(rejectReason);
        app.setRejectionReason(rejectReason);
        loanRepository.save(app);

        completeKogitoTask(app, "MakerReview", Map.of(
                "makerDecision", "REJECT",
                "comments", rejectReason
        ));

        notificationService.notifyMakerRejection(app);
    }

    public void requestEditByMaker(Long applicationId, String makerName, String makerEmail, String editReason) {
        LoanApplication app = findOrThrow(applicationId);

        app.setMakerName(makerName);
        app.setMakerEmail(makerEmail);
        app.requestEditByMaker(editReason);
        loanRepository.save(app);

        completeKogitoTask(app, "MakerReview", Map.of(
                "makerDecision", "REQUEST_EDIT",
                "comments", editReason
        ));

        notificationService.notifyEditRequest(app);
    }

    // =====================================================================
    // CHECKER DECISIONS
    // =====================================================================

    public void approveByChecker(Long applicationId, String checkerName, String checkerEmail, String comments) {
        LoanApplication app = findOrThrow(applicationId);

        app.setCheckerName(checkerName);
        app.setCheckerEmail(checkerEmail);
        app.approveByChecker();
        app.setCheckerComments(comments);
        loanRepository.save(app);

        completeKogitoTask(app, "CheckerReview", Map.of(
                "checkerDecision", "APPROVE",
                "comments", comments != null ? comments : ""
        ));

        notificationService.notifyApproval(app);
    }

    public void rejectByChecker(Long applicationId, String checkerName, String checkerEmail, String rejectReason) {
        LoanApplication app = findOrThrow(applicationId);

        app.setCheckerName(checkerName);
        app.setCheckerEmail(checkerEmail);
        app.rejectByChecker(rejectReason);
        app.setRejectionReason(rejectReason);
        loanRepository.save(app);

        completeKogitoTask(app, "CheckerReview", Map.of(
                "checkerDecision", "REJECT",
                "comments", rejectReason
        ));

        notificationService.notifyRejection(app);
    }

    public void sendBackToMaker(Long applicationId, String checkerName, String checkerEmail, String reason) {
        LoanApplication app = findOrThrow(applicationId);

        app.setCheckerName(checkerName);
        app.setCheckerEmail(checkerEmail);
        app.sendBackToMaker(reason);
        loanRepository.save(app);

        completeKogitoTask(app, "CheckerReview", Map.of(
                "checkerDecision", "SEND_BACK",
                "comments", reason
        ));

        notificationService.notifySendBackToMaker(app);
    }

    // =====================================================================
    // EDIT / RESUBMIT
    // =====================================================================

    public void resubmitAfterEdit(Long applicationId, LoanApplication updatedApp) {
        LoanApplication app = findOrThrow(applicationId);

        app.setApplicantName(updatedApp.getApplicantName());
        app.setApplicantEmail(updatedApp.getApplicantEmail());
        app.setApplicantPhone(updatedApp.getApplicantPhone());
        app.setLoanAmount(updatedApp.getLoanAmount());
        app.setLoanTerm(updatedApp.getLoanTerm());
        app.setAnnualIncome(updatedApp.getAnnualIncome());
        app.setCreditScore(updatedApp.getCreditScore());
        app.setEmploymentStatus(updatedApp.getEmploymentStatus());
        app.setTermsAccepted(updatedApp.isTermsAccepted());

        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setCurrentStage("MAKER_REVIEW");
        app.setMakerDecision(null);
        app.setMakerComments(null);

        loanRepository.save(app);
        notificationService.notifyResubmission(app);
    }

    // =====================================================================
    // QUERY HELPERS
    // =====================================================================

    public LoanApplication getApplicationDetails(Long applicationId) {
        return findOrThrow(applicationId);
    }

    public boolean isInMakerReview(Long applicationId) {
        return "MAKER_REVIEW".equals(findOrThrow(applicationId).getCurrentStage());
    }

    public boolean isInCheckerReview(Long applicationId) {
        return "CHECKER_REVIEW".equals(findOrThrow(applicationId).getCurrentStage());
    }

    public boolean isEditRequested(Long applicationId) {
        return "EDIT_REQUESTED".equals(findOrThrow(applicationId).getCurrentStage());
    }

    public String getAuditTrail(Long applicationId) {
        LoanApplication app = findOrThrow(applicationId);
        StringBuilder audit = new StringBuilder();
        audit.append("=== APPLICATION AUDIT TRAIL ===\n");
        audit.append(String.format("Application ID  : %d\n", app.getId()));
        audit.append(String.format("Applicant       : %s (%s)\n", app.getApplicantName(), app.getApplicantEmail()));
        audit.append(String.format("Status          : %s\n", app.getStatus().getDisplayName()));
        audit.append(String.format("Current Stage   : %s\n", app.getCurrentStage()));
        audit.append(String.format("Loan Amount     : %s\n", app.getLoanAmount()));
        audit.append(String.format("Credit Score    : %d\n", app.getCreditScore()));
        audit.append(String.format("Edit Count      : %d\n", app.getEditCount()));
        audit.append(String.format("Process ID      : %s\n", app.getProcessInstanceId()));
        audit.append(String.format("Maker Assigned  : %s\n", app.getMakerAssignee()));
        audit.append(String.format("Checker Assigned: %s\n", app.getCheckerAssignee()));

        if (app.getMakerReviewDate() != null) {
            audit.append("\nMAKER REVIEW:\n");
            audit.append(String.format("  Reviewer : %s (%s)\n", app.getMakerName(), app.getMakerEmail()));
            audit.append(String.format("  Decision : %s\n", app.getMakerDecision()));
            audit.append(String.format("  Date     : %s\n", app.getMakerReviewDate()));
            audit.append(String.format("  Comments : %s\n", app.getMakerComments()));
        }

        if (app.getCheckerReviewDate() != null) {
            audit.append("\nCHECKER REVIEW:\n");
            audit.append(String.format("  Reviewer : %s (%s)\n", app.getCheckerName(), app.getCheckerEmail()));
            audit.append(String.format("  Decision : %s\n", app.getCheckerDecision()));
            audit.append(String.format("  Date     : %s\n", app.getCheckerReviewDate()));
            audit.append(String.format("  Comments : %s\n", app.getCheckerComments()));
        }

        if (app.getEditReason() != null) {
            audit.append(String.format("\nEDIT REQUEST:\n  Reason: %s\n", app.getEditReason()));
        }

        return audit.toString();
    }

    // =====================================================================
    // PRIVATE: Kogito task completion
    // =====================================================================

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void completeKogitoTask(LoanApplication app, String taskName, Map<String, Object> outputVars) {
        if (app.getProcessInstanceId() == null) {
            log.warn("No processInstanceId on application {}. Skipping Kogito task completion.", app.getId());
            return;
        }

        try {
            Process typedProcess = (Process) loanApprovalProcess;
            Optional<ProcessInstance> instanceOpt = typedProcess.instances()
                    .findById(app.getProcessInstanceId());

            if (instanceOpt.isEmpty()) {
                log.warn("Process instance {} not found.", app.getProcessInstanceId());
                return;
            }

            ProcessInstance instance = instanceOpt.get();
            List<WorkItem> workItems = instance.workItems();

            Optional<WorkItem> taskItem = workItems.stream()
                    .filter(wi -> taskName.equals(wi.getName()) ||
                            wi.getName().replace(" ", "").equalsIgnoreCase(taskName))
                    .findFirst();

            if (taskItem.isPresent()) {
                instance.completeWorkItem(taskItem.get().getId(), outputVars);
                log.info("Completed Kogito task '{}' for process instance {}", taskName, app.getProcessInstanceId());
            } else {
                log.warn("Task '{}' not found in active work items for instance {}. Active tasks: {}",
                        taskName, app.getProcessInstanceId(),
                        workItems.stream().map(WorkItem::getName).toList());
            }

        } catch (Exception e) {
            log.error("Failed to complete Kogito task '{}' for process instance {}: {}",
                    taskName, app.getProcessInstanceId(), e.getMessage(), e);
        }
    }

    private LoanApplication findOrThrow(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found: " + id));
    }
}