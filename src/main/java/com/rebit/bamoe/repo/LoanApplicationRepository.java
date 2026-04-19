package com.rebit.bamoe.repo;

import com.rebit.bamoe.entity.ApplicationStatus;
import com.rebit.bamoe.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByProcessInstanceId(String processInstanceId);

    /** All applications currently sitting in a given stage. */
    List<LoanApplication> findByCurrentStage(String currentStage);

    /** All applications assigned to a specific maker, in MAKER_REVIEW stage. */
    List<LoanApplication> findByCurrentStageAndMakerAssignee(String currentStage, String makerAssignee);

    /** All applications assigned to a specific checker, in CHECKER_REVIEW stage. */
    List<LoanApplication> findByCurrentStageAndCheckerAssignee(String currentStage, String checkerAssignee);

    List<LoanApplication> findByStatus(ApplicationStatus status);

    /** Applications that were sent back for editing, belonging to a specific applicant. */
    List<LoanApplication> findByCurrentStageAndApplicantEmail(String currentStage, String applicantEmail);
}