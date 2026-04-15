package com.rebit.bamoe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rebit.bamoe.entity.LoanApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

@Service
public class FormService {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Load form definition from JSON file
     */
    public Map<String, Object> getFormDefinition(String formName) throws IOException {
        String filePath = String.format("classpath:forms/%s.json", formName);
        // In production, load from resources or database
        // This is a simplified example
        return Map.of(
                "formName", formName,
                "fields", "[]"
        );
    }

    /**
     * Validate form submission data
     */
    public ValidationResult validateFormSubmission(String formName, Map<String, Object> formData) {
        ValidationResult result = new ValidationResult();

        // Required field validation
        if (!formData.containsKey("applicantName") ||
                formData.get("applicantName").toString().trim().isEmpty()) {
            result.addError("applicantName", "Full name is required");
        }

        if (!formData.containsKey("applicantEmail")) {
            result.addError("applicantEmail", "Email is required");
        }

        if (!formData.containsKey("loanAmount")) {
            result.addError("loanAmount", "Loan amount is required");
        }

        if (!formData.containsKey("annualIncome")) {
            result.addError("annualIncome", "Annual income is required");
        }

        if (!formData.containsKey("creditScore")) {
            result.addError("creditScore", "Credit score is required");
        }

        // Number range validation
        try {
            Long loanAmount = Long.parseLong(formData.get("loanAmount").toString());
            if (loanAmount < 50000 || loanAmount > 5000000) {
                result.addError("loanAmount", "Loan amount must be between 50000 and 5000000");
            }
        } catch (NumberFormatException e) {
            result.addError("loanAmount", "Loan amount must be a valid number");
        }

        try {
            Integer creditScore = Integer.parseInt(formData.get("creditScore").toString());
            if (creditScore < 300 || creditScore > 850) {
                result.addError("creditScore", "Credit score must be between 300 and 850");
            }
        } catch (NumberFormatException e) {
            result.addError("creditScore", "Credit score must be a valid number");
        }

        return result;
    }

    /**
     * Convert form submission to domain object
     */
    public LoanApplication convertFormToApplication(Map<String, Object> formData) {

        LoanApplication app = new LoanApplication();

        app.setApplicantName((String) formData.get("applicantName"));
        app.setApplicantEmail((String) formData.get("applicantEmail"));
        app.setApplicantPhone((String) formData.get("applicantPhone"));

        app.setLoanAmount(new BigDecimal(formData.get("loanAmount").toString()));
        app.setLoanTerm(Integer.parseInt(formData.get("loanTerm").toString()));
        app.setAnnualIncome(new BigDecimal(formData.get("annualIncome").toString()));
        app.setCreditScore(Integer.parseInt(formData.get("creditScore").toString()));

        // ✅ NEW
        app.setEmploymentStatus((String) formData.get("employmentStatus"));

        // ✅ FIX checkbox
        Object terms = formData.get("termsAccepted");
        boolean accepted = false;

        if (terms instanceof Boolean) {
            accepted = (Boolean) terms;
        } else if (terms instanceof String) {
            accepted = "true".equalsIgnoreCase((String) terms)
                    || "on".equalsIgnoreCase((String) terms);
        }

        app.setTermsAccepted(accepted);

        return app;
    }
}
