package com.rebit.bamoe.service;

import com.rebit.bamoe.entity.ApplicationStatus;
import com.rebit.bamoe.entity.LoanApplication;
import com.rebit.bamoe.repo.LoanApplicationRepository;
import org.kie.kogito.Model;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.Processes;
import org.kie.kogito.process.WorkItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
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
 * Step 2 is what was missing before. Without completing the Kogito task,
 * the BPMN process just sits frozen at the user task forever.
 */
@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    @Autowired
    private LoanApplicationRepository loanRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private Processes processes;

    // =====================================================================
    // MAKER DECISIONS
    // =====================================================================

    /**
     * Maker approves the application.
     * BPMN: MakerDecisionGateway takes the "APPROVE" branch → CheckerReview task opens.
     */
    public void approveByMaker(Long applicationId, String makerName, String makerEmail, String comments) {
        LoanApplication app = findOrThrow(applicationId);

        // Update DB record
        app.setMakerName(makerName);
        app.setMakerEmail(makerEmail);
        app.setMakerDecision("APPROVE");
        app.setMakerComments(comments);
        app.setMakerReviewDate(LocalDateTime.now());
        app.moveToCheckerReview();
        loanRepository.save(app);

        // Complete the Kogito "Maker Review" user task with makerDecision = APPROVE
        completeKogitoTask(app, "MakerReview", Map.of(
                "makerDecision", "APPROVE",
                "comments", comments != null ? comments : ""
        ));

        notificationService.notifyMakerApproval(app);
    }

    /**
     * Maker rejects the application.
     * BPMN: MakerDecisionGateway takes the "REJECT" branch → EndRejectedByMaker.
     */
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

    /**
     * Maker requests the applicant to edit and resubmit.
     * BPMN: MakerDecisionGateway → NotifyEditRequested service task → loops back to MakerReview.
     */
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

    /**
     * Checker approves the application (final approval).
     * BPMN: CheckerDecisionGateway → EndApproved.
     */
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

    /**
     * Checker rejects the application.
     * BPMN: CheckerDecisionGateway → EndRejectedByChecker.
     */
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

    /**
     * Checker sends application back to Maker for further review.
     * BPMN: CheckerDecisionGateway → loops back to MakerReview.
     */
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

        // Reset back to maker review
        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setCurrentStage("MAKER_REVIEW");
        app.setMakerDecision(null);
        app.setMakerComments(null);

        loanRepository.save(app);
        notificationService.notifyResubmission(app);
        // No Kogito task to complete here - the BPMN loop already returned to MakerReview
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

    /**
     * Finds the active user task with the given name on the given process instance,
     * then completes it with the provided output variables.
     *
     * This is what actually advances the BPMN engine.
     *
     * @param app         the loan application (must have processInstanceId set)
     * @param taskName    the name attribute of the userTask in the BPMN (e.g. "MakerReview")
     * @param outputVars  variables to write back into the process (e.g. makerDecision)
     */
    @SuppressWarnings("unchecked")
    private void completeKogitoTask(LoanApplication app, String taskName, Map<String, Object> outputVars) {
        if (app.getProcessInstanceId() == null) {
            log.warn("No processInstanceId on application {}. Skipping Kogito task completion.", app.getId());
            return;
        }

        try {
            // Determine which process this application belongs to
            // We try all registered processes - for production, store workflowId in LoanApplication too
            for (Process<?> process : getAllProcesses()) {
                Process<Model> typedProcess = (Process<Model>) process;
                Optional<ProcessInstance<Model>> instanceOpt = typedProcess
                        .instances()
                        .findById(app.getProcessInstanceId());

                if (instanceOpt.isEmpty()) continue;

                ProcessInstance<Model> instance = instanceOpt.get();

                // Find the active work item (user task) matching the task name
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
                return;
            }

            log.warn("Process instance {} not found in any registered Kogito process.", app.getProcessInstanceId());

        } catch (Exception e) {
            // Log but don't throw - the DB record is already updated.
            // A failed Kogito completion means the BPMN is out of sync with DB,
            // which you can recover by checking the management console.
            log.error("Failed to complete Kogito task '{}' for process instance {}: {}",
                    taskName, app.getProcessInstanceId(), e.getMessage(), e);
        }
    }

    /**
     * Returns all registered Kogito processes.
     * Processes.processById() only works if you know the ID,
     * so we iterate all of them to find the one with the matching instance.
     */
    private Iterable<? extends Process<?>> getAllProcesses() {
        // Kogito's Processes registry doesn't expose a list() method directly,
        // so we use the fact that it's Iterable in some versions,
        // or we can just try the known process IDs.
        // The simplest approach: store workflowId in LoanApplication (see TODO below)
        // For now, try the one we know about.
        Process<?> p = processes.processById("LoanApprovalProcess");
        return p != null ? List.of(p) : List.of();
    }

    private LoanApplication findOrThrow(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found: " + id));
    }
}