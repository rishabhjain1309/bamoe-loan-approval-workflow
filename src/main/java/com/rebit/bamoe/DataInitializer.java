package com.rebit.bamoe;

import com.rebit.bamoe.entity.FormConfig;
import com.rebit.bamoe.repo.FormConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with a default FormConfig on startup if none exists.
 *
 * This means you don't need to manually POST to /api/forms/config
 * before testing. You can always update it via the API.
 *
 * For production: remove this class and manage configs via the admin API.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private FormConfigRepository formConfigRepository;

    @Override
    public void run(String... args) {
        if (formConfigRepository.findByFormName("loanApplicationForm").isEmpty()) {
            FormConfig config = new FormConfig();
            config.setFormName("loanApplicationForm");
            config.setFormTitle("Loan Application");
            config.setWorkflowId("LoanApprovalProcess");  // Must match BPMN id="LoanApprovalProcess"
            config.setMakerAssignee("maker1");             // Change to real username
            config.setCheckerAssignee("checker1");         // Change to real username
            config.setActive(true);
            formConfigRepository.save(config);
            log.info("Created default FormConfig: loanApplicationForm -> LoanApprovalProcess (maker1/checker1)");
            log.info("Update via: POST /api/forms/config with your real maker/checker usernames");
        } else {
            log.info("FormConfig already exists for loanApplicationForm");
        }
    }
}