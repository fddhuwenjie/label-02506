package com.clinic.service;

import com.clinic.entity.Registration;
import com.clinic.entity.RegistrationRule;
import com.clinic.entity.RegistrationSlot;
import com.clinic.entity.User;
import com.clinic.repository.RegistrationRepository;
import com.clinic.repository.RegistrationRuleRepository;
import com.clinic.repository.RegistrationSlotRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final RegistrationSlotRepository slotRepository;
    
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
                var slotOpt = slotRepository.findByDoctorIdAndRegDateAndTimePeriod(doctorId, date, timePeriod);
                if (slotOpt.isPresent()) {
                    return slotOpt.get().getCurrentCount() < slotOpt.get().getMaxCount();
                }
                return 0 < rule.getMaxCount();
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
        List<RegistrationRule> rules = ruleRepository.findByDoctorIdAndWeekDayAndStatus(doctor.getId(), weekDay, 1);
        
        RegistrationRule matchingRule = null;
        BigDecimal fee = BigDecimal.ZERO;
        for (RegistrationRule rule : rules) {
            if (rule.getTimePeriod().equals(timePeriod)) {
                matchingRule = rule;
                fee = rule.getFee();
                break;
            }
        }
        
        if (matchingRule == null) {
            throw new IllegalStateException("该医生此时段无排班");
        }
        
        RegistrationSlot slot = getOrCreateSlotWithLock(doctor.getId(), date, timePeriod, matchingRule.getMaxCount());
        
        if (slot.getCurrentCount() >= slot.getMaxCount()) {
            throw new IllegalStateException("号源已满");
        }
        
        slot.setCurrentCount(slot.getCurrentCount() + 1);
        slot.setCurrentQueueNo(slot.getCurrentQueueNo() + 1);
        slotRepository.save(slot);
        
        Integer queueNo = slot.getCurrentQueueNo();
        
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
    
    private RegistrationSlot getOrCreateSlotWithLock(Long doctorId, LocalDate date, Integer timePeriod, Integer maxCount) {
        var slotOpt = slotRepository.findByDoctorIdAndRegDateAndTimePeriodWithLock(doctorId, date, timePeriod);
        if (slotOpt.isPresent()) {
            return slotOpt.get();
        }
        
        Integer existingCount = registrationRepository.countByDoctorAndDateAndPeriod(doctorId, date, timePeriod);
        Integer existingMaxQueueNo = registrationRepository.findMaxQueueNo(doctorId, date, timePeriod);
        
        try {
            RegistrationSlot newSlot = new RegistrationSlot();
            newSlot.setDoctorId(doctorId);
            newSlot.setRegDate(date);
            newSlot.setTimePeriod(timePeriod);
            newSlot.setMaxCount(maxCount);
            newSlot.setCurrentCount(existingCount);
            newSlot.setCurrentQueueNo(existingMaxQueueNo);
            slotRepository.save(newSlot);
            slotRepository.flush();
        } catch (DataIntegrityViolationException e) {
        }
        
        return slotRepository.findByDoctorIdAndRegDateAndTimePeriodWithLock(doctorId, date, timePeriod)
                .orElseThrow(() -> new IllegalStateException("号源槽位创建失败"));
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
        
        RegistrationSlot slot = slotRepository.findByDoctorIdAndRegDateAndTimePeriodWithLock(
                reg.getDoctor().getId(), reg.getRegDate(), reg.getTimePeriod())
                .orElse(null);
        
        if (slot != null && slot.getCurrentCount() > 0) {
            slot.setCurrentCount(slot.getCurrentCount() - 1);
            slotRepository.save(slot);
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
