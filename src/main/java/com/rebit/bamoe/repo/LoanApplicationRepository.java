package com.rebit.bamoe.repo;

import com.rebit.bamoe.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    LoanApplication findByProcessInstanceId(String processInstanceId);
}
