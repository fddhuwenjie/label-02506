package com.clinic.service;

import com.clinic.entity.Registration;
import com.clinic.entity.RegistrationRule;
import com.clinic.entity.User;
import com.clinic.repository.RegistrationRepository;
import com.clinic.repository.RegistrationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final RegistrationRuleRepository ruleRepository;
    
    public List<Registration> findByPatient(Long patientId) {
        return registrationRepository.findByPatientIdOrderByRegDateDesc(patientId);
    }
    
    public List<Registration> findByDoctorAndDate(Long doctorId, LocalDate date) {
        return registrationRepository.findByDoctorIdAndRegDateOrderByQueueNo(doctorId, date);
    }
    
    public List<Registration> findByDate(LocalDate date) {
        return registrationRepository.findByRegDateOrderByQueueNo(date);
    }
    
    public Registration findById(Long id) {
        return registrationRepository.findById(id).orElse(null);
    }
    
    public List<RegistrationRule> getDoctorRules(Long doctorId) {
        return ruleRepository.findByDoctorIdAndStatus(doctorId, 1);
    }
    
    public boolean canRegister(Long doctorId, LocalDate date, Integer timePeriod) {
        int weekDay = date.getDayOfWeek().getValue();
        List<RegistrationRule> rules = ruleRepository.findByDoctorIdAndWeekDayAndStatus(doctorId, weekDay, 1);
        
        for (RegistrationRule rule : rules) {
            if (rule.getTimePeriod().equals(timePeriod)) {
                Integer count = registrationRepository.countByDoctorAndDateAndPeriod(doctorId, date, timePeriod);
                return count < rule.getMaxCount();
            }
        }
        return false;
    }

    public boolean hasDuplicateRegistration(Long patientId, Long doctorId, LocalDate date, Integer timePeriod) {
        return registrationRepository.existsDuplicateActiveRegistration(patientId, doctorId, date, timePeriod);
    }
    
    @Transactional
    public Registration createRegistration(User patient, User doctor, LocalDate date, Integer timePeriod, Integer source) {
        if (hasDuplicateRegistration(patient.getId(), doctor.getId(), date, timePeriod)) {
            throw new IllegalStateException("同一病人同一医生同一时段已存在有效挂号");
        }

        int weekDay = date.getDayOfWeek().getValue();
        List<RegistrationRule> rules = ruleRepository.findByDoctorIdAndWeekDayAndStatus(doctor.getId(), weekDay, 1);
        
        BigDecimal fee = BigDecimal.ZERO;
        for (RegistrationRule rule : rules) {
            if (rule.getTimePeriod().equals(timePeriod)) {
                fee = rule.getFee();
                break;
            }
        }
        
        Integer queueNo = registrationRepository.findMaxQueueNo(doctor.getId(), date, timePeriod) + 1;
        
        Registration reg = new Registration();
        reg.setRegNo(generateRegNo());
        reg.setPatient(patient);
        reg.setDoctor(doctor);
        reg.setRegDate(date);
        reg.setTimePeriod(timePeriod);
        reg.setQueueNo(queueNo);
        reg.setFee(fee);
        reg.setStatus(0);
        reg.setSource(source);
        
        return registrationRepository.save(reg);
    }
    
    private String generateRegNo() {
        return "REG" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) 
               + System.currentTimeMillis() % 100000;
    }
    
    @Transactional
    public void cancelRegistration(Long id, String reason) {
        Registration reg = registrationRepository.findById(id).orElseThrow();
        reg.setStatus(3);
        reg.setCancelReason(reason);
        registrationRepository.save(reg);
    }
    
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Registration reg = registrationRepository.findById(id).orElseThrow();
        reg.setStatus(status);
        registrationRepository.save(reg);
    }
    
    public Long countTodayRegistrations() {
        return registrationRepository.countByDate(LocalDate.now());
    }
    
    // 挂号规则管理
    public List<RegistrationRule> findAllRules() {
        return ruleRepository.findAll();
    }
    
    public RegistrationRule findRuleById(Long id) {
        return ruleRepository.findById(id).orElse(null);
    }
    
    @Transactional
    public void saveRule(RegistrationRule rule) {
        ruleRepository.save(rule);
    }
    
    @Transactional
    public void toggleRuleStatus(Long id) {
        RegistrationRule rule = ruleRepository.findById(id).orElseThrow();
        rule.setStatus(rule.getStatus() == 1 ? 0 : 1);
        ruleRepository.save(rule);
    }
    
    @Transactional
    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
    }

    public List<Registration> findRecentRegistrations(LocalDate startDate, LocalDate endDate) {
        return registrationRepository.findByDateRangeOrderByDateDesc(startDate, endDate);
    }
}
