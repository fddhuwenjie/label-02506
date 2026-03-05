package com.clinic.repository;

import com.clinic.entity.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    Optional<PatientProfile> findByUserId(Long userId);
    Optional<PatientProfile> findByMedicalCardNo(String medicalCardNo);
    boolean existsByIdCard(String idCard);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.medicalCardNo, 11) AS int)), 0) FROM PatientProfile p WHERE p.medicalCardNo LIKE ?1%")
    Integer findMaxCardNoByPrefix(String prefix);
}
