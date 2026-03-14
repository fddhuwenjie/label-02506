package com.clinic.service;

import com.clinic.entity.*;
import com.clinic.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionService {
    private final PrescriptionRepository prescriptionRepository;
    private final MedicineService medicineService;
    
    @Transactional(readOnly = true)
    public Prescription findById(Long id) {
        Prescription p = prescriptionRepository.findById(id).orElse(null);
        if (p != null && p.getDetails() != null) {
            p.getDetails().size(); // 强制加载 details
        }
        return p;
    }
    
    public Prescription findByMedicalRecord(Long recordId) {
        return prescriptionRepository.findByMedicalRecordId(recordId).orElse(null);
    }
    
    public List<Prescription> findByPatient(Long patientId) {
        return prescriptionRepository.findByPatientIdOrderByCreateTimeDesc(patientId);
    }
    
    public List<Prescription> findPendingAudit() {
        return prescriptionRepository.findByStatus(0);
    }
    
    public List<Prescription> findPendingAuditByDoctor(Long doctorId) {
        return prescriptionRepository.findByDoctorIdAndStatus(doctorId, 0);
    }
    
    public List<Prescription> findAll() {
        return prescriptionRepository.findAllByOrderByCreateTimeDesc();
    }
    
    public List<Prescription> findByDoctor(Long doctorId) {
        return prescriptionRepository.findByDoctorIdOrderByCreateTimeDesc(doctorId);
    }
    
    @Transactional
    public Prescription createPrescription(MedicalRecord record, List<PrescriptionDetail> details) {
        if (record == null) {
            throw new IllegalArgumentException("病历记录不能为空");
        }
        if (record.getPatient() == null) {
            throw new IllegalArgumentException("病历缺少病人信息");
        }
        if (record.getDoctor() == null) {
            throw new IllegalArgumentException("病历缺少医生信息");
        }
        if (details == null || details.isEmpty()) {
            throw new IllegalArgumentException("处方明细不能为空");
        }
        
        Prescription prescription = new Prescription();
        prescription.setPrescriptionNo(generatePrescriptionNo());
        prescription.setMedicalRecord(record);
        prescription.setPatient(record.getPatient());
        prescription.setDoctor(record.getDoctor());
        prescription.setDiagnosis(record.getDiagnosis());
        prescription.setStatus(0);
        
        BigDecimal total = BigDecimal.ZERO;
        for (PrescriptionDetail detail : details) {
            if (detail.getUnitPrice() == null) {
                throw new IllegalArgumentException("药品单价不能为空");
            }
            if (detail.getQuantity() == null || detail.getQuantity() <= 0) {
                throw new IllegalArgumentException("药品数量必须大于0");
            }
            detail.setPrescription(prescription);
            detail.setAmount(detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())));
            total = total.add(detail.getAmount());
        }
        prescription.setTotalAmount(total);
        prescription.setDetails(details);
        
        return prescriptionRepository.save(prescription);
    }
    
    private String generatePrescriptionNo() {
        return "RX" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
               + System.currentTimeMillis()
               + ThreadLocalRandom.current().nextInt(100, 1000);
    }
    
    @Transactional
    public void auditPrescription(Long id, User auditDoctor) {
        Prescription prescription = prescriptionRepository.findById(id).orElseThrow();
        if (prescription.getStatus() != null && prescription.getStatus() >= 1) {
            throw new IllegalStateException("该处方已审核，无需重复审核");
        }
        prescription.setStatus(1);
        prescription.setAuditDoctor(auditDoctor);
        prescription.setAuditTime(LocalDateTime.now());
        prescriptionRepository.save(prescription);
    }
    
    @Transactional
    public void dispensePrescription(Long id) {
        Prescription prescription = prescriptionRepository.findById(id).orElseThrow();
        if (prescription.getStatus() != null && prescription.getStatus() >= 2) {
            throw new IllegalStateException("该处方已发药，无需重复发药");
        }
        if (prescription.getStatus() == null || prescription.getStatus() < 1) {
            throw new IllegalStateException("处方未审核，无法发药");
        }
        
        // 扣减库存
        for (PrescriptionDetail detail : prescription.getDetails()) {
            medicineService.updateStock(detail.getMedicine().getId(), -detail.getQuantity());
        }
        
        prescription.setStatus(2);
        prescription.setDispenseTime(LocalDateTime.now());
        prescriptionRepository.save(prescription);
    }
}
