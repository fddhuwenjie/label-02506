package com.clinic.repository;

import com.clinic.dto.MedicineStatDTO;
import com.clinic.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Optional<Prescription> findByPrescriptionNo(String prescriptionNo);
    List<Prescription> findByPatientIdOrderByCreateTimeDesc(Long patientId);
    List<Prescription> findByDoctorIdOrderByCreateTimeDesc(Long doctorId);
    List<Prescription> findAllByOrderByCreateTimeDesc();
    List<Prescription> findByStatus(Integer status);
    List<Prescription> findByDoctorIdAndStatus(Long doctorId, Integer status);
    Optional<Prescription> findByMedicalRecordId(Long medicalRecordId);
    
    @Query("SELECT p FROM Prescription p WHERE p.createTime BETWEEN ?1 AND ?2")
    List<Prescription> findByDateRange(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.createTime BETWEEN ?1 AND ?2")
    Long countByDateRange(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT new com.clinic.dto.MedicineStatDTO(d.medicine.id, d.medicineName, SUM(d.quantity), SUM(d.amount)) " +
           "FROM PrescriptionDetail d WHERE d.prescription.createTime BETWEEN ?1 AND ?2 " +
           "GROUP BY d.medicine.id, d.medicineName ORDER BY SUM(d.quantity) DESC LIMIT ?3")
    List<MedicineStatDTO> findTopMedicines(LocalDateTime start, LocalDateTime end, int limit);
}
