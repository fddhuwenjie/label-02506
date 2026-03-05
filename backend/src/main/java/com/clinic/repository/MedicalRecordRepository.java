package com.clinic.repository;

import com.clinic.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    Optional<MedicalRecord> findByRecordNo(String recordNo);
    Optional<MedicalRecord> findByRegistrationId(Long registrationId);
    List<MedicalRecord> findByPatientIdOrderByVisitTimeDesc(Long patientId);
    List<MedicalRecord> findByDoctorIdOrderByVisitTimeDesc(Long doctorId);
    
    @Query("SELECT m FROM MedicalRecord m WHERE m.isFollowUp = 1 AND m.followUpDate = ?1")
    List<MedicalRecord> findFollowUpByDate(LocalDate date);
    
    @Query("SELECT m FROM MedicalRecord m WHERE m.patient.id = ?1 AND m.isFollowUp = 1 AND m.followUpDate >= ?2")
    List<MedicalRecord> findUpcomingFollowUps(Long patientId, LocalDate fromDate);
}
