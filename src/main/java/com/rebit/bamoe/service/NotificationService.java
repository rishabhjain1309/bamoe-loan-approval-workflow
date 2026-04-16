package com.rebit.bamoe.service;

import com.rebit.bamoe.entity.LoanApplication;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Notify applicant that maker approved their application
     */
    public void notifyMakerApproval(LoanApplication app) {
        String subject = "Loan Application - Maker Review Complete";
        String body = String.format(
                "Hello %s,\n\n" +
                        "Good news! Your loan application (ID: %d) has been approved by our internal team.\n" +
                        "Your application is now under final review by our checker.\n" +
                        "We will notify you of the final decision shortly.\n\n" +
                        "Best regards,\nLoan Department",
                app.getApplicantName(), app.getId()
        );
        sendEmail(app.getApplicantEmail(), subject, body);
    }

    /**
     * Notify applicant of maker rejection
     */
    public void notifyMakerRejection(LoanApplication app) {
        String subject = "Loan Application - Rejected";
        String body = String.format(
                "Hello %s,\n\n" +
                        "Unfortunately, your loan application (ID: %d) has been rejected during internal review.\n" +
                        "Reason: %s\n\n" +
                        "If you have questions, please contact us.\n\n" +
                        "Best regards,\nLoan Department",
                app.getApplicantName(), app.getId(), app.getMakerComments()
        );
        sendEmail(app.getApplicantEmail(), subject, body);
    }

    /**
     * Notify applicant that maker requested edits
     */
    public void notifyEditRequest(LoanApplication app) {
        String subject = "Loan Application - Changes Required";
        String body = String.format(
                "Hello %s,\n\n" +
                        "Your loan application (ID: %d) requires some changes.\n" +
                        "Please review the feedback and resubmit your application.\n\n" +
                        "Feedback: %s\n\n" +
                        "Edit Count: %d\n\n" +
                        "Please resubmit at your earliest convenience.\n\n" +
                        "Best regards,\nLoan Department",
                app.getApplicantName(), app.getId(), app.getEditReason(), app.getEditCount()
        );
        sendEmail(app.getApplicantEmail(), subject, body);
    }

    /**
     * Notify applicant of final approval
     */
    public void notifyApproval(LoanApplication app) {
        String subject = "Loan Application - APPROVED!";
        String body = String.format(
                "Hello %s,\n\n" +
                        "Congratulations! Your loan application (ID: %d) has been APPROVED.\n" +
                        "Loan Amount: %s\n" +
                        "Term: %d months\n\n" +
                        "Your approval letter is attached. Please proceed with the next steps.\n\n" +
                        "Best regards,\nLoan Department",
                app.getApplicantName(), app.getId(), app.getLoanAmount(), app.getLoanTerm()
        );
        sendEmail(app.getApplicantEmail(), subject, body);

        // Also notify maker and checker
        sendEmail(app.getMakerEmail(), "Application Approved: " + app.getId(),
                "Application " + app.getId() + " has been finalized and approved.");
        sendEmail(app.getCheckerEmail(), "Application Finalized: " + app.getId(),
                "Application " + app.getId() + " has been approved and marked as final.");
    }

    /**
     * Notify of rejection
     */
    public void notifyRejection(LoanApplication app) {
        String subject = "Loan Application - Rejected";
        String body = String.format(
                "Hello %s,\n\n" +
                        "Unfortunately, your loan application (ID: %d) has been rejected.\n" +
                        "Reason: %s\n\n" +
                        "Best regards,\nLoan Department",
                app.getApplicantName(), app.getId(), app.getCheckerComments()
        );
        sendEmail(app.getApplicantEmail(), subject, body);
    }

    /**
     * Notify maker that checker sent application back
     */
    public void notifySendBackToMaker(LoanApplication app) {
        String subject = "Application Returned for Review: " + app.getId();
        String body = String.format(
                "Hello %s,\n\n" +
                        "Application %d has been returned by the checker for further review.\n" +
                        "Reason: %s\n\n" +
                        "Please review and resubmit.\n\n" +
                        "Best regards,\nLoan Department",
                app.getMakerName(), app.getId(), app.getCheckerComments()
        );
        sendEmail(app.getMakerEmail(), subject, body);
    }

    /**
     * Notify maker of application resubmission
     */
    public void notifyResubmission(LoanApplication app) {
        String subject = "Application Resubmitted: " + app.getId();
        String body = String.format(
                "Hello,\n\n" +
                        "Application %d has been resubmitted after requested edits.\n" +
                        "Applicant: %s\n" +
                        "Please review the updated application.\n\n" +
                        "Best regards,\nSystem",
                app.getId(), app.getApplicantName()
        );
        sendEmail(app.getMakerEmail(), subject, body);
    }

    /**
     * Send email (stub - implement with JavaMail, SendGrid, etc.)
     */
    private void sendEmail(String to, String subject, String body) {
        logger.info("EMAIL TO: {}", to);
        logger.info("SUBJECT: {}", subject);
        logger.info("BODY: {}", body);

        // TODO: Implement actual email sending
        // Options:
        // 1. JavaMail API
        // 2. Spring Mail (JavaMailSender)
        // 3. SendGrid
        // 4. AWS SES
    }
}