package com.rebit.bamoe.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Stores the mapping between a form and its workflow configuration.
 *
 * When a user creates a form (via admin), they choose:
 *   - Which BPMN process to trigger (workflowId must match the BPMN process id="...")
 *   - Who is the default maker (username)
 *   - Who is the default checker (username)
 *
 * At submission time, FormController reads this config to know:
 *   1. Which process to start
 *   2. Which usernames to inject as makerAssignee / checkerAssignee variables
 */
@Entity
@Table(name = "form_configs")
@Data
public class FormConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier for the form, e.g. "loanApplicationForm".
     * This is what the frontend POSTs to /forms/{formName}/submit.
     */
    @Column(name = "form_name", nullable = false, unique = true)
    private String formName;

    /**
     * Human-readable form title shown in the UI.
     */
    @Column(name = "form_title", nullable = false)
    private String formTitle;

    /**
     * BPMN process ID to trigger when this form is submitted.
     * Must exactly match the id="..." attribute in the .bpmn2 file.
     * Examples: "LoanApprovalProcess", "MortgageApprovalProcess"
     */
    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    /**
     * Username of the Maker assigned to review submissions of this form.
     * Injected as the "makerAssignee" process variable so Kogito assigns
     * the Maker Review user task to this person.
     */
    @Column(name = "maker_assignee", nullable = false)
    private String makerAssignee;

    /**
     * Username of the Checker assigned to final-review submissions.
     * Injected as the "checkerAssignee" process variable.
     */
    @Column(name = "checker_assignee", nullable = false)
    private String checkerAssignee;

    /**
     * Whether this form config is active. Inactive configs reject new submissions.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}