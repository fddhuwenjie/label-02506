package com.clinic.service;

import com.clinic.entity.Payment;
import com.clinic.entity.Prescription;
import com.clinic.entity.Registration;
import com.clinic.entity.User;
import com.clinic.repository.PaymentRepository;
import com.clinic.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PrescriptionRepository prescriptionRepository;
    
    public Payment findById(Long id) {
        return paymentRepository.findById(id).orElse(null);
    }
    
    public List<Payment> findByPatient(Long patientId) {
        return paymentRepository.findByPatientIdOrderByCreateTimeDesc(patientId);
    }
    
    public List<Payment> findUnpaidByPatient(Long patientId) {
        return paymentRepository.findUnpaidByPatient(patientId);
    }

    public List<Payment> findByRegistration(Long registrationId) {
        return paymentRepository.findByRegistrationIdOrderByCreateTimeDesc(registrationId);
    }

    public List<Payment> findPendingForDoctor() {
        return paymentRepository.findByStatusInOrderByCreateTimeDesc(Arrays.asList(0, 2));
    }

    public List<Payment> findAllForDoctor() {
        return paymentRepository.findAllByOrderByCreateTimeDesc();
    }
    
    public List<Payment> findByDoctor(Long doctorId) {
        return paymentRepository.findByDoctorIdOrderByCreateTimeDesc(doctorId);
    }

    public List<Payment> findArrearsForDoctor() {
        return paymentRepository.findByStatusOrderByCreateTimeDesc(2);
    }
    
    @Transactional
    public Payment createPayment(User patient, Integer feeType, BigDecimal amount) {
        if (patient == null) {
            throw new IllegalArgumentException("病人信息不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("费用金额必须大于0");
        }
        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setPatient(patient);
        payment.setFeeType(feeType);
        payment.setTotalAmount(amount);
        payment.setPaidAmount(BigDecimal.ZERO);
        payment.setStatus(0);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment createPayment(User patient, Registration registration, Integer feeType, BigDecimal amount, String remark, User operator) {
        if (patient == null) {
            throw new IllegalArgumentException("病人信息不能为空");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("费用金额必须大于0");
        }
        Payment payment = new Payment();
        payment.setPaymentNo(generatePaymentNo());
        payment.setPatient(patient);
        payment.setRegistration(registration);
        payment.setFeeType(feeType);
        payment.setTotalAmount(amount);
        payment.setPaidAmount(BigDecimal.ZERO);
        payment.setStatus(0);
        payment.setRemark(remark);
        payment.setOperator(operator);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment createPrescriptionPaymentIfAbsent(Long prescriptionId, Registration registration, User operator) {
        if (prescriptionId == null) {
            throw new IllegalArgumentException("处方信息无效");
        }
        Prescription prescription = prescriptionRepository.findById(prescriptionId).orElseThrow();
        return paymentRepository.findByPrescriptionId(prescriptionId).orElseGet(() -> {
            Payment payment = new Payment();
            payment.setPaymentNo(generatePaymentNo());
            payment.setPatient(prescription.getPatient());
            payment.setRegistration(registration);
            payment.setPrescription(prescription);
            payment.setFeeType(3); // 药品费
            payment.setTotalAmount(prescription.getTotalAmount());
            payment.setPaidAmount(BigDecimal.ZERO);
            payment.setStatus(0);
            payment.setRemark("处方缴费：" + prescription.getPrescriptionNo());
            payment.setOperator(operator);
            return paymentRepository.save(payment);
        });
    }
    
    private String generatePaymentNo() {
        return "PAY" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
               + System.currentTimeMillis()
               + ThreadLocalRandom.current().nextInt(100, 1000);
    }
    
    @Transactional
    public void pay(Long id, BigDecimal amount, Integer method, User operator) {
        Payment payment = paymentRepository.findById(id).orElseThrow();
        
        if (payment.getStatus() == 1) {
            throw new IllegalStateException("该缴费单已支付完成，无需重复支付");
        }
        
        BigDecimal currentPaid = payment.getPaidAmount() != null ? payment.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = payment.getTotalAmount() != null ? payment.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal remaining = totalAmount.subtract(currentPaid);
        
        if (amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("支付金额超过剩余应付金额（剩余：" + remaining + "元）");
        }
        
        BigDecimal nextPaid = currentPaid.add(amount);
        payment.setPaidAmount(nextPaid);
        payment.setPaymentMethod(method);
        payment.setOperator(operator);
        payment.setPayTime(LocalDateTime.now());
        
        if (nextPaid.compareTo(totalAmount) >= 0) {
            payment.setStatus(1);
        } else {
            payment.setStatus(2);
        }
        paymentRepository.save(payment);
    }

    @Transactional
    public void registerArrears(Long id, String arrearsRemark, User operator) {
        Payment payment = paymentRepository.findById(id).orElseThrow();
        if (payment.getStatus() == 1) {
            throw new IllegalStateException("该缴费单已支付完成，无需登记欠费");
        }
        String oldRemark = payment.getRemark() == null ? "" : payment.getRemark();
        String append = "欠费登记：" + arrearsRemark;
        payment.setRemark(oldRemark.isBlank() ? append : oldRemark + "；" + append);
        payment.setOperator(operator);
        paymentRepository.save(payment);
    }
    
    public BigDecimal getTodayRevenue() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        BigDecimal sum = paymentRepository.sumPaidAmountByDateRange(start, end);
        return sum != null ? sum : BigDecimal.ZERO;
    }
}
