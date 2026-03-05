package com.clinic.service;

import com.clinic.entity.MedicalRecord;
import com.clinic.entity.Registration;
import com.clinic.repository.MedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {
    private final MedicalRecordRepository recordRepository;
    
    public MedicalRecord findById(Long id) {
        return recordRepository.findById(id).orElse(null);
    }
    
    public MedicalRecord findByRegistration(Long registrationId) {
        return recordRepository.findByRegistrationId(registrationId).orElse(null);
    }
    
    public List<MedicalRecord> findByPatient(Long patientId) {
        return recordRepository.findByPatientIdOrderByVisitTimeDesc(patientId);
    }
    
    public List<MedicalRecord> findByDoctor(Long doctorId) {
        return recordRepository.findByDoctorIdOrderByVisitTimeDesc(doctorId);
    }
    
    public List<MedicalRecord> findTodayFollowUps() {
        return recordRepository.findFollowUpByDate(LocalDate.now());
    }
    
    public List<MedicalRecord> findUpcomingFollowUps(Long patientId) {
        return recordRepository.findUpcomingFollowUps(patientId, LocalDate.now());
    }
    
    @Transactional
    public MedicalRecord createRecord(Registration registration) {
        MedicalRecord record = new MedicalRecord();
        record.setRecordNo(generateRecordNo());
        record.setRegistration(registration);
        record.setPatient(registration.getPatient());
        record.setDoctor(registration.getDoctor());
        record.setVisitTime(LocalDateTime.now());
        return recordRepository.save(record);
    }
    
    private String generateRecordNo() {
        return "MR" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
               + System.currentTimeMillis()
               + ThreadLocalRandom.current().nextInt(100, 1000);
    }
    
    @Transactional
    public void updateRecord(MedicalRecord record) {
        recordRepository.save(record);
    }
    
    @Transactional
    public MedicalRecord save(MedicalRecord record) {
        return recordRepository.save(record);
    }
}
