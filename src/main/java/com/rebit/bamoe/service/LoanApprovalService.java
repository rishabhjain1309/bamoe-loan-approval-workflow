package com.rebit.bamoe.service;


import com.rebit.bamoe.entity.ApplicationStatus;
import com.rebit.bamoe.entity.LoanApplication;
import com.rebit.bamoe.repo.LoanApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class LoanApprovalService {

    @Autowired
    private LoanApplicationRepository loanRepository;

    /**
     * Verify applicant's income based on credit score and loan-to-income ratio
     */
    public boolean verifyIncome(LoanApplication application) {
        // Debt-to-Income Ratio check
        BigDecimal monthlyIncome = application.getAnnualIncome()
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        BigDecimal monthlyPayment = calculateMonthlyPayment(
                application.getLoanAmount(),
                application.getLoanTerm()
        );

        BigDecimal debtToIncomeRatio = monthlyPayment
                .divide(monthlyIncome, 4, RoundingMode.HALF_UP);

        // Credit Score check
        boolean creditScoreValid = application.getCreditScore() >= 620;

        // DTI Ratio should be less than 0.43 (43%)
        boolean dtiValid = debtToIncomeRatio.compareTo(BigDecimal.valueOf(0.43)) <= 0;

        return creditScoreValid && dtiValid;
    }

    /**
     * Calculate monthly EMI (Equated Monthly Installment)
     */
    private BigDecimal calculateMonthlyPayment(BigDecimal principal, Integer months) {

        BigDecimal annualRate = BigDecimal.valueOf(0.05); // 5%

        // monthly rate with precision
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        // (1 + r)
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);

        // (1 + r)^n
        BigDecimal power = onePlusR.pow(months);

        // numerator = P * r * (1+r)^n
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(power);

        // denominator = (1+r)^n - 1
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        // FINAL EMI
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP); // ✅ FIX
    }

    /**
     * Complete the loan application
     */
    public void completeLoanApplication(LoanApplication application, boolean approved, String reason) {
        application.setStatus(approved ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED);
        if (!approved) {
            application.setRejectionReason(reason);
        }
        loanRepository.save(application);
    }
}
