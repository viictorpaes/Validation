package com.cesar.validation.repository;

import com.cesar.validation.entity.DocumentSubmission;
import com.cesar.validation.enums.ValidationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentSubmissionRepository extends JpaRepository<DocumentSubmission, UUID> {

    List<DocumentSubmission> findAllByOrderByCreatedAtDesc();

    List<DocumentSubmission> findByValidationStatusOrderByCreatedAtDesc(ValidationStatus status);

    List<DocumentSubmission> findTop5ByOrderByCreatedAtDesc();

    long countByValidationStatus(ValidationStatus status);
}
