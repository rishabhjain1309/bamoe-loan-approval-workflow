package com.rebit.bamoe.controller;

import com.rebit.bamoe.entity.FormConfig;
import com.rebit.bamoe.entity.LoanApplication;
import com.rebit.bamoe.repo.FormConfigRepository;
import com.rebit.bamoe.repo.LoanApplicationRepository;
import com.rebit.bamoe.service.FormService;
import com.rebit.bamoe.service.ValidationResult;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.Processes;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private Processes processes;

    // =====================================================================
    // FORM CONFIG MANAGEMENT
    // =====================================================================

    /**
     * Create or update a form->workflow mapping.
     *
     * POST /api/forms/config
     * {
     *   "formName": "loanApplicationForm",
     *   "formTitle": "Loan Application",
     *   "workflowId": "LoanApprovalProcess",
     *   "makerAssignee": "maker1",
     *   "checkerAssignee": "checker1"
     * }
     */
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

    /**
     * Submit a form. Validates -> saves to DB -> starts Kogito BPMN process.
     *
     * POST /api/forms/{formName}/submit
     */
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

        // 2. Load form config (which workflow + who are the assignees)
        FormConfig config = formConfigRepository.findByFormNameAndActiveTrue(formName)
                .orElseThrow(() -> new RuntimeException(
                        "No active form config for: " + formName +
                                ". POST /api/forms/config first."));

        // 3. Save application to DB
        LoanApplication application = formService.convertFormToApplication(formData);
        application.submitForMakerReview(config.getMakerAssignee(), config.getCheckerAssignee());
        LoanApplication savedApp = loanRepository.save(application);

        try {
            // 4. Build the process variables map
            Map<String, Object> processVars = new HashMap<>();
            processVars.put("applicationId",   savedApp.getId());
            processVars.put("makerAssignee",   config.getMakerAssignee());
            processVars.put("checkerAssignee", config.getCheckerAssignee());
            processVars.put("makerDecision",   "");
            processVars.put("checkerDecision", "");
            processVars.put("comments",        "");
            processVars.put("approved",        false);

            // 5. Start the Kogito process.
            //
            // WHY @SuppressWarnings("rawtypes") here:
            //   Process<T> has two createInstance overloads:
            //     createInstance(T model)          -- where T is the generated type
            //     createInstance(Model model)      -- the interface version
            //   When you cast to Process<Model>, javac sees BOTH as equally valid
            //   for a Model argument -> "ambiguous method call" compile error.
            //
            //   The fix: use the raw type Process (no <T>). With a raw type,
            //   javac picks the most specific overload without ambiguity.
            //   The @SuppressWarnings suppresses the "raw types" warning.
            @SuppressWarnings("rawtypes")
            org.kie.kogito.process.Process rawProcess =
                    processes.processById(config.getWorkflowId());

            if (rawProcess == null) {
                throw new RuntimeException(
                        "Kogito process not found: '" + config.getWorkflowId() + "'. " +
                                "Check: (1) kogito-maven-plugin ran, " +
                                "(2) BPMN file has id=\"" + config.getWorkflowId() + "\", " +
                                "(3) target/generated-sources/kogito/ contains the generated classes.");
            }

            // createModel() returns the concrete generated class (LoanApprovalProcessModel)
            // fromMap() fills its fields from our variables map
            org.kie.kogito.Model model = (org.kie.kogito.Model) rawProcess.createModel();
            model.fromMap(processVars);

            // Now createInstance(model) is unambiguous: we're passing the
            // concrete generated type, not the raw Model interface
            @SuppressWarnings("unchecked")
            ProcessInstance<?> instance = rawProcess.createInstance(model);
            instance.start();

            // 6. Store process instance ID so we can complete tasks later
            savedApp.setProcessInstanceId(instance.id());
            loanRepository.save(savedApp);

            return ResponseEntity.ok(Map.of(
                    "success",          true,
                    "message",          "Submitted! Awaiting review by " + config.getMakerAssignee(),
                    "applicationId",    savedApp.getId(),
                    "processInstanceId", instance.id(),
                    "workflowId",       config.getWorkflowId(),
                    "makerAssignee",    config.getMakerAssignee(),
                    "checkerAssignee",  config.getCheckerAssignee(),
                    "status",           savedApp.getStatus().name(),
                    "currentStage",     savedApp.getCurrentStage()
            ));

        } catch (Exception e) {
            savedApp.setCurrentStage("WORKFLOW_START_FAILED");
            loanRepository.save(savedApp);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Form saved but workflow failed: " + e.getMessage(),
                    "applicationId", savedApp.getId(),
                    "hint", "Run: dir target\\generated-sources\\kogito\\ and check LoanApprovalProcessModel.java exists"
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

    // Legacy aliases so old URLs still work
    @PostMapping("/loan-application/submit")
    public ResponseEntity<?> submitLoanApplicationForm(@RequestBody Map<String, Object> formData) {
        return submitForm("loanApplicationForm", formData);
    }

    @GetMapping("/loan-application")
    public ResponseEntity<?> getLoanApplicationForm() {
        return getFormDefinition("loanApplicationForm");
    }
}