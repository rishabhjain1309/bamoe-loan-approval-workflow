package com.rebit.bamoe.controller;

import com.rebit.bamoe.entity.LoanApplication;
import com.rebit.bamoe.repo.LoanApplicationRepository;
import com.rebit.bamoe.service.FormService;
import com.rebit.bamoe.service.ValidationResult;
import com.rebit.bamoe.service.LoanApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/forms")
@CrossOrigin(origins = "*")
public class FormController {

    @Autowired
    private FormService formService;

    @Autowired
    private LoanApplicationRepository loanRepository;

    @Autowired
    private LoanApprovalService loanApprovalService;

    /**
     * GET /api/forms/loan-application
     * Returns the form schema for rendering on the frontend
     */
    @GetMapping("/loan-application")
    public ResponseEntity<Map<String, Object>> getLoanApplicationForm() throws IOException {
        Map<String, Object> formDefinition = formService.getFormDefinition("loanApplicationForm");
        return ResponseEntity.ok(formDefinition);
    }

    /**
     * POST /api/forms/loan-application/submit
     * Handles form submission and creates workflow instance
     */
    @PostMapping("/loan-application/submit")
    public ResponseEntity<?> submitLoanApplicationForm(@RequestBody Map<String, Object> formData) {

        // Validate form data
        ValidationResult validation = formService.validateFormSubmission("loanApplicationForm", formData);

        if (!validation.isValid()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "errors", validation.getErrors()
                    ));
        }

        // Convert form data to domain object
        LoanApplication application = formService.convertFormToApplication(formData);

        // Save application to database
        LoanApplication savedApp = loanRepository.save(application);

        // TODO: Start Kogito workflow process
        // ProcessInstance instance = loanApprovalProcessInstance.start(
        //     Map.of("application", savedApp)
        // );
        // savedApp.setProcessInstanceId(instance.getId());
        // loanRepository.save(savedApp);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Loan application submitted successfully",
                "applicationId", savedApp.getId(),
                "status", "SUBMITTED"
        ));
    }

    /**
     * POST /api/forms/loan-application/validate
     * Real-time validation without submission
     */
    @PostMapping("/loan-application/validate")
    public ResponseEntity<?> validateLoanApplicationForm(@RequestBody Map<String, Object> formData) {
        ValidationResult validation = formService.validateFormSubmission("loanApplicationForm", formData);

        return ResponseEntity.ok(Map.of(
                "isValid", validation.isValid(),
                "errors", validation.getErrors()
        ));
    }
}
