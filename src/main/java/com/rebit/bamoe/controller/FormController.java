package com.rebit.bamoe.controller;

import com.rebit.bamoe.entity.FormConfig;
import com.rebit.bamoe.entity.LoanApplication;
import com.rebit.bamoe.repo.FormConfigRepository;
import com.rebit.bamoe.repo.LoanApplicationRepository;
import com.rebit.bamoe.service.FormService;
import com.rebit.bamoe.service.ValidationResult;
import org.kie.kogito.Model;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
    private FormConfigRepository formConfigRepository;

    /**
     * Inject the generated LoanApprovalProcessProcess bean directly by qualifier.
     * This avoids the Processes registry lookup entirely.
     */
    @Autowired
    @Qualifier("LoanApprovalProcess")
    private Process<? extends Model> loanApprovalProcess;

    // =====================================================================
    // FORM CONFIG MANAGEMENT
    // =====================================================================

    @PostMapping("/config")
    public ResponseEntity<?> createFormConfig(@RequestBody FormConfig config) {
        formConfigRepository.findByFormName(config.getFormName())
                .ifPresent(existing -> config.setId(existing.getId()));
        return ResponseEntity.ok(formConfigRepository.save(config));
    }

    @GetMapping("/config")
    public ResponseEntity<List<FormConfig>> listFormConfigs() {
        return ResponseEntity.ok(formConfigRepository.findAll());
    }

    @GetMapping("/{formName}")
    public ResponseEntity<?> getFormDefinition(@PathVariable String formName) {
        return formConfigRepository.findByFormNameAndActiveTrue(formName)
                .map(config -> {
                    Map<String, Object> form = new HashMap<>();
                    form.put("formName", config.getFormName());
                    form.put("title", config.getFormTitle());
                    form.put("workflowId", config.getWorkflowId());
                    return ResponseEntity.ok(form);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // =====================================================================
    // FORM SUBMISSION
    // =====================================================================

    @PostMapping("/{formName}/submit")
    public ResponseEntity<?> submitForm(
            @PathVariable String formName,
            @RequestBody Map<String, Object> formData) {

        // 1. Validate
        ValidationResult validation = formService.validateFormSubmission(formName, formData);
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "errors", validation.getErrors()
            ));
        }

        // 2. Load form config
        FormConfig config = formConfigRepository.findByFormNameAndActiveTrue(formName)
                .orElseThrow(() -> new RuntimeException(
                        "No active form config for: " + formName +
                                ". POST /api/forms/config first."));

        // 3. Save application to DB
        LoanApplication application = formService.convertFormToApplication(formData);
        application.submitForMakerReview(config.getMakerAssignee(), config.getCheckerAssignee());
        LoanApplication savedApp = loanRepository.save(application);

        try {
            // 4. Build process variables map
            Map<String, Object> processVars = new HashMap<>();
            processVars.put("applicationId",   savedApp.getId());
            processVars.put("makerAssignee",   config.getMakerAssignee());
            processVars.put("checkerAssignee", config.getCheckerAssignee());
            processVars.put("makerDecision",   "");
            processVars.put("checkerDecision", "");
            processVars.put("comments",        "");
            processVars.put("approved",        false);

            // 5. Start the Kogito process using the directly injected bean
            @SuppressWarnings({"rawtypes", "unchecked"})
            Process rawProcess = (Process) loanApprovalProcess;

            org.kie.kogito.Model model = (org.kie.kogito.Model) rawProcess.createModel();
            model.fromMap(processVars);

            @SuppressWarnings("unchecked")
            ProcessInstance<?> instance = rawProcess.createInstance(model);
            instance.start();

            // 6. Store process instance ID
            savedApp.setProcessInstanceId(instance.id());
            loanRepository.save(savedApp);

            return ResponseEntity.ok(Map.of(
                    "success",           true,
                    "message",           "Submitted! Awaiting review by " + config.getMakerAssignee(),
                    "applicationId",     savedApp.getId(),
                    "processInstanceId", instance.id(),
                    "workflowId",        config.getWorkflowId(),
                    "makerAssignee",     config.getMakerAssignee(),
                    "checkerAssignee",   config.getCheckerAssignee(),
                    "status",            savedApp.getStatus().name(),
                    "currentStage",      savedApp.getCurrentStage()
            ));

        } catch (Exception e) {
            savedApp.setCurrentStage("WORKFLOW_START_FAILED");
            loanRepository.save(savedApp);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success",       false,
                    "message",       "Form saved but workflow failed: " + e.getMessage(),
                    "applicationId", savedApp.getId()
            ));
        }
    }

    @PostMapping("/{formName}/validate")
    public ResponseEntity<?> validateForm(
            @PathVariable String formName,
            @RequestBody Map<String, Object> formData) {
        ValidationResult v = formService.validateFormSubmission(formName, formData);
        return ResponseEntity.ok(Map.of("isValid", v.isValid(), "errors", v.getErrors()));
    }

    // Legacy aliases
    @PostMapping("/loan-application/submit")
    public ResponseEntity<?> submitLoanApplicationForm(@RequestBody Map<String, Object> formData) {
        return submitForm("loanApplicationForm", formData);
    }

    @GetMapping("/loan-application")
    public ResponseEntity<?> getLoanApplicationForm() {
        return getFormDefinition("loanApplicationForm");
    }
}