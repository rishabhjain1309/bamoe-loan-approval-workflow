package com.rebit.bamoe.controller;


import com.rebit.bamoe.entity.ApplicationStatus;
import com.rebit.bamoe.entity.LoanApplication;
import com.rebit.bamoe.repo.LoanApplicationRepository;
import com.rebit.bamoe.service.LoanApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/loan")
public class LoanController {

    @Autowired
    private LoanApplicationRepository loanRepository;

    @Autowired
    private LoanApprovalService loanService;

    /**
     * Submit a new loan application
     * POST /api/loan/apply
     */
    @PostMapping("/apply")
    public ResponseEntity<LoanApplication> submitApplication(
            @RequestBody LoanApplication application) {

        // Save the application
        LoanApplication saved = loanRepository.save(application);

        // TODO: Start Kogito workflow process here
        // ProcessInstance instance = new LoanApprovalProcess()
        //     .withApplicantName(application.getApplicantName())
        //     .start();
        // saved.setProcessInstanceId(instance.getId());
        // loanRepository.save(saved);

        saved.setStatus(ApplicationStatus.SUBMITTED);
        return ResponseEntity.ok(saved);
    }

    /**
     * Get application by ID
     * GET /api/loan/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<LoanApplication> getApplication(@PathVariable Long id) {
        return loanRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Verify income for an application
     * POST /api/loan/{id}/verify
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<Boolean> verifyIncome(@PathVariable Long id) {
        return loanRepository.findById(id)
                .map(app -> {
                    boolean isValid = loanService.verifyIncome(app);
                    return ResponseEntity.ok(isValid);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Complete application approval/rejection
     * POST /api/loan/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<LoanApplication> completeApplication(
            @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reason) {

        return loanRepository.findById(id)
                .map(app -> {
                    loanService.completeLoanApplication(app, approved, reason);
                    return ResponseEntity.ok(app);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
