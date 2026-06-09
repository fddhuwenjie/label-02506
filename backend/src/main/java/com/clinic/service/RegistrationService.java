package com.clinic.service;

import com.clinic.entity.Registration;
import com.clinic.entity.RegistrationRule;
import com.clinic.entity.User;
import com.clinic.exception.BusinessException;
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
    
    public boolean hasSchedule(Long doctorId, LocalDate date, Integer timePeriod) {
        int weekDay = date.getDayOfWeek().getValue();
        List<RegistrationRule> rules = ruleRepository.findByDoctorIdAndWeekDayAndStatus(doctorId, weekDay, 1);
        
        for (RegistrationRule rule : rules) {
            if (rule.getTimePeriod().equals(timePeriod)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasDuplicateRegistration(Long patientId, Long doctorId, LocalDate date, Integer timePeriod) {
        return registrationRepository.existsDuplicateActiveRegistration(patientId, doctorId, date, timePeriod);
    }
    
    @Transactional
    public Registration createRegistration(User patient, User doctor, LocalDate date, Integer timePeriod, Integer source) {
        if (patient == null) {
            throw new IllegalArgumentException("病人信息不能为空");
        }
        if (doctor == null) {
            throw new IllegalArgumentException("医生信息不能为空");
        }
        if (date == null) {
            throw new IllegalArgumentException("挂号日期不能为空");
        }
        if (timePeriod == null) {
            throw new IllegalArgumentException("时段不能为空");
        }
        if (hasDuplicateRegistration(patient.getId(), doctor.getId(), date, timePeriod)) {
            throw new IllegalStateException("同一病人同一医生同一时段已存在有效挂号");
        }

        int weekDay = date.getDayOfWeek().getValue();

        RegistrationRule rule = ruleRepository.findByDoctorAndWeekDayAndPeriodWithLock(doctor.getId(), weekDay, timePeriod)
                .orElseThrow(() -> new BusinessException("该医生此时段暂无排班"));

        Integer currentCount = registrationRepository.countByDoctorAndDateAndPeriod(doctor.getId(), date, timePeriod);
        if (currentCount >= rule.getMaxCount()) {
            throw new BusinessException("该时段号源已约满");
        }

        Integer queueNo = registrationRepository.findMaxQueueNo(doctor.getId(), date, timePeriod) + 1;

        Registration reg = new Registration();
        reg.setRegNo(generateRegNo());
        reg.setPatient(patient);
        reg.setDoctor(doctor);
        reg.setRegDate(date);
        reg.setTimePeriod(timePeriod);
        reg.setQueueNo(queueNo);
        reg.setFee(rule.getFee());
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
        if (reg.getStatus() != 0) {
            throw new IllegalStateException("只有待就诊状态的挂号才能取消");
        }
        reg.setStatus(3);
        reg.setCancelReason(reason);
        registrationRepository.save(reg);
    }
    
    @Transactional
    public void callPatient(Long id) {
        Registration reg = registrationRepository.findById(id).orElseThrow();
        if (reg.getStatus() != 0) {
            throw new IllegalStateException("只有待就诊状态的挂号才能叫号");
        }
        reg.setStatus(1);
        registrationRepository.save(reg);
    }
    
    @Transactional
    public void startVisit(Long id) {
        Registration reg = registrationRepository.findById(id).orElseThrow();
        if (reg.getStatus() != 0 && reg.getStatus() != 1) {
            throw new IllegalStateException("该挂号状态不允许开始接诊");
        }
        reg.setStatus(1);
        registrationRepository.save(reg);
    }
    
    @Transactional
    public void completeVisit(Long id) {
        Registration reg = registrationRepository.findById(id).orElseThrow();
        if (reg.getStatus() != 1) {
            throw new IllegalStateException("只有就诊中状态的挂号才能完诊");
        }
        reg.setStatus(2);
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
    
    public List<RegistrationRule> findRulesByDoctor(Long doctorId) {
        return ruleRepository.findByDoctorId(doctorId);
    }
    
    public RegistrationRule findRuleById(Long id) {
        return ruleRepository.findById(id).orElse(null);
    }
    
    @Transactional
    public void saveRule(RegistrationRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("规则不能为空");
        }
        if (rule.getDoctor() == null) {
            throw new IllegalArgumentException("必须指定医生");
        }
        if (rule.getWeekDay() == null || rule.getWeekDay() < 1 || rule.getWeekDay() > 7) {
            throw new IllegalArgumentException("星期必须在1-7之间");
        }
        if (rule.getTimePeriod() == null || (rule.getTimePeriod() != 1 && rule.getTimePeriod() != 2)) {
            throw new IllegalArgumentException("时段必须是1（上午）或2（下午）");
        }
        if (rule.getMaxCount() == null || rule.getMaxCount() <= 0) {
            throw new IllegalArgumentException("最大挂号数必须大于0");
        }
        if (rule.getFee() == null || rule.getFee().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("挂号费不能为负数");
        }
        ruleRepository.save(rule);
    }
    
    @Transactional
    public void toggleRuleStatus(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("规则ID不能为空");
        }
        RegistrationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("规则不存在"));
        rule.setStatus(rule.getStatus() == 1 ? 0 : 1);
        ruleRepository.save(rule);
    }
    
    @Transactional
    public void deleteRule(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("规则ID不能为空");
        }
        if (!ruleRepository.existsById(id)) {
            throw new IllegalArgumentException("规则不存在");
        }
        ruleRepository.deleteById(id);
    }

    public List<Registration> findRecentRegistrations(LocalDate startDate, LocalDate endDate) {
        return registrationRepository.findByDateRangeOrderByDateDesc(startDate, endDate);
    }
    
    public List<Registration> findByDoctorAndDateRange(Long doctorId, LocalDate startDate, LocalDate endDate) {
        return registrationRepository.findByDoctorIdAndDateRangeOrderByDateDesc(doctorId, startDate, endDate);
    }
}
