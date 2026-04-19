package com.rebit.bamoe.repo;

import com.rebit.bamoe.entity.FormConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FormConfigRepository extends JpaRepository<FormConfig, Long> {

    /**
     * Finds the active config for a given form name.
     * Used by FormController at submission time to look up which
     * workflow to start and who the maker/checker are.
     */
    Optional<FormConfig> findByFormNameAndActiveTrue(String formName);

    Optional<FormConfig> findByFormName(String formName);
}