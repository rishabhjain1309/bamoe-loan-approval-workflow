package com.rebit.bamoe.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.math.BigDecimal;
import javax.persistence.*;

@Entity
@Table(name = "loan_applications")
@Data
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;

    @Column(nullable = false)
    private BigDecimal loanAmount;

    @Column(nullable = false)
    private Integer loanTerm; // months

    @Column(nullable = false)
    private BigDecimal annualIncome;

    @Column(nullable = false)
    private Integer creditScore;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private String rejectionReason;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDate createdDate;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "employment_Status")
    private String employmentStatus;

    @Column(name = "terms_accepted")
    private boolean termsAccepted;

    // Constructors
    public LoanApplication() {
        this.status = ApplicationStatus.NEW;
        this.createdDate = LocalDate.now();
    }

}

