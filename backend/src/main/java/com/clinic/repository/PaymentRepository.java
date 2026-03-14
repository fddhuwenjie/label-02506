package com.clinic.repository;

import com.clinic.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentNo(String paymentNo);
    Optional<Payment> findByPrescriptionId(Long prescriptionId);
    List<Payment> findByPatientIdOrderByCreateTimeDesc(Long patientId);
    List<Payment> findByPatientIdAndStatus(Long patientId, Integer status);
    
    @Query("SELECT SUM(p.paidAmount) FROM Payment p WHERE p.status = 1 AND p.payTime BETWEEN ?1 AND ?2")
    BigDecimal sumPaidAmountByDateRange(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT SUM(p.paidAmount) FROM Payment p WHERE p.status = 1 AND p.feeType = ?1 AND p.payTime BETWEEN ?2 AND ?3")
    BigDecimal sumByFeeTypeAndDateRange(Integer feeType, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT p FROM Payment p WHERE p.patient.id = ?1 AND p.status IN (0, 2)")
    List<Payment> findUnpaidByPatient(Long patientId);

    List<Payment> findByRegistrationIdOrderByCreateTimeDesc(Long registrationId);

    List<Payment> findByStatusInOrderByCreateTimeDesc(List<Integer> statuses);

    List<Payment> findByStatusOrderByCreateTimeDesc(Integer status);

    List<Payment> findAllByOrderByCreateTimeDesc();
    
    @Query("SELECT p FROM Payment p WHERE p.registration.doctor.id = ?1 ORDER BY p.createTime DESC")
    List<Payment> findByDoctorIdOrderByCreateTimeDesc(Long doctorId);
}
