package com.clinic.repository;

import com.clinic.entity.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Optional<Registration> findByRegNo(String regNo);
    
    List<Registration> findByPatientIdOrderByRegDateDesc(Long patientId);
    
    List<Registration> findByDoctorIdAndRegDateOrderByQueueNo(Long doctorId, LocalDate regDate);
    
    List<Registration> findByRegDateAndStatus(LocalDate regDate, Integer status);
    
    @Query("SELECT r FROM Registration r WHERE r.regDate = ?1 ORDER BY r.queueNo")
    List<Registration> findByRegDateOrderByQueueNo(LocalDate regDate);
    
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.doctor.id = ?1 AND r.regDate = ?2 AND r.timePeriod = ?3 AND r.status != 3")
    Integer countByDoctorAndDateAndPeriod(Long doctorId, LocalDate regDate, Integer timePeriod);
    
    @Query("SELECT COALESCE(MAX(r.queueNo), 0) FROM Registration r WHERE r.doctor.id = ?1 AND r.regDate = ?2 AND r.timePeriod = ?3")
    Integer findMaxQueueNo(Long doctorId, LocalDate regDate, Integer timePeriod);
    
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.regDate = ?1")
    Long countByDate(LocalDate date);
    
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.regDate = ?1 AND r.status = 2")
    Long countCompletedByDate(LocalDate date);
    
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.regDate BETWEEN ?1 AND ?2")
    Long countByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.regDate BETWEEN ?1 AND ?2 AND r.status = 2")
    Long countCompletedByDateRange(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.regDate BETWEEN ?1 AND ?2 AND r.status = 3")
    Long countCancelledByDateRange(LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(r) > 0 FROM Registration r WHERE r.patient.id = ?1 AND r.doctor.id = ?2 AND r.regDate = ?3 AND r.timePeriod = ?4 AND r.status <> 3")
    boolean existsDuplicateActiveRegistration(Long patientId, Long doctorId, LocalDate regDate, Integer timePeriod);

    @Query("SELECT r FROM Registration r WHERE r.regDate BETWEEN ?1 AND ?2 AND r.status <> 3 ORDER BY r.regDate DESC, r.queueNo")
    List<Registration> findByDateRangeOrderByDateDesc(LocalDate startDate, LocalDate endDate);
}
