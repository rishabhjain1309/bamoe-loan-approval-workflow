package com.rebit.bamoe.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import javax.persistence.*;

@Entity
@Table(name = "loan_applications")
@Data
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // ============ APPLICANT INFORMATION ============
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;

    // ============ LOAN DETAILS ============
    @Column(nullable = false)
    private BigDecimal loanAmount;

    @Column(nullable = false)
    private Integer loanTerm; // months

    @Column(nullable = false)
    private BigDecimal annualIncome;

    @Column(nullable = false)
    private Integer creditScore;

    private String employmentStatus;
    private boolean termsAccepted;

    // ============ WORKFLOW STATUS ============
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Column(name = "current_stage")
    private String currentStage; // SUBMITTED, MAKER_REVIEW, CHECKER_REVIEW

    // ============ MAKER REVIEW DETAILS ============
    @Column(name = "maker_name")
    private String makerName;

    @Column(name = "maker_email")
    private String makerEmail;

    @Column(name = "maker_decision")
    private String makerDecision; // APPROVED, REJECTED, EDIT_REQUESTED

    @Column(name = "maker_comments", length = 2000)
    private String makerComments;

    @Column(name = "maker_review_date")
    private LocalDateTime makerReviewDate;

    // ============ CHECKER REVIEW DETAILS ============
    @Column(name = "checker_name")
    private String checkerName;

    @Column(name = "checker_email")
    private String checkerEmail;

    @Column(name = "checker_decision")
    private String checkerDecision; // APPROVED, REJECTED, SEND_BACK

    @Column(name = "checker_comments", length = 2000)
    private String checkerComments;

    @Column(name = "checker_review_date")
    private LocalDateTime checkerReviewDate;

    // ============ EDIT TRACKING ============
    @Column(name = "edit_reason", length = 1000)
    private String editReason;

    @Column(name = "edit_count")
    private Integer editCount = 0;

    // ============ AUDIT ============
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDate createdDate;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    private String rejectionReason;

    // ============ CONSTRUCTORS ============
    public LoanApplication() {
        this.status = ApplicationStatus.NEW;
        this.currentStage = "NEW";
        this.createdDate = LocalDate.now();
        this.editCount = 0;
    }

    // ============ HELPER METHODS ============
    public void submitForMakerReview() {
        this.status = ApplicationStatus.SUBMITTED;
        this.currentStage = "MAKER_REVIEW";
    }

    public void approveBrMaker() {
        this.makerDecision = "APPROVED";
        this.makerReviewDate = LocalDateTime.now();
        this.currentStage = "CHECKER_REVIEW";
        this.status = ApplicationStatus.MAKER_APPROVED;
    }

    public void rejectByMaker(String reason) {
        this.makerDecision = "REJECTED";
        this.makerComments = reason;
        this.makerReviewDate = LocalDateTime.now();
        this.status = ApplicationStatus.MAKER_REJECTED;
    }

    public void requestEditByMaker(String reason) {
        this.makerDecision = "EDIT_REQUESTED";
        this.editReason = reason;
        this.makerReviewDate = LocalDateTime.now();
        this.currentStage = "EDIT_REQUESTED";
        this.editCount++;
    }

    public void approveByChecker() {
        this.checkerDecision = "APPROVED";
        this.checkerReviewDate = LocalDateTime.now();
        this.status = ApplicationStatus.APPROVED;
    }

    public void rejectByChecker(String reason) {
        this.checkerDecision = "REJECTED";
        this.checkerComments = reason;
        this.checkerReviewDate = LocalDateTime.now();
        this.status = ApplicationStatus.REJECTED;
    }

    public void sendBackToMaker(String reason) {
        this.checkerDecision = "SEND_BACK";
        this.checkerComments = reason;
        this.checkerReviewDate = LocalDateTime.now();
        this.currentStage = "MAKER_REVIEW";
        this.status = ApplicationStatus.MAKER_REVIEW;
    }
}