package com.clinic.repository;

import com.clinic.entity.RegistrationRule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RegistrationRuleRepository extends JpaRepository<RegistrationRule, Long> {
    List<RegistrationRule> findByDoctorIdAndStatus(Long doctorId, Integer status);
    List<RegistrationRule> findByDoctorIdAndWeekDayAndStatus(Long doctorId, Integer weekDay, Integer status);
    List<RegistrationRule> findByDoctorId(Long doctorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RegistrationRule r WHERE r.doctor.id = ?1 AND r.weekDay = ?2 AND r.timePeriod = ?3 AND r.status = 1")
    List<RegistrationRule> findByDoctorAndWeekDayAndPeriodForUpdate(Long doctorId, Integer weekDay, Integer timePeriod);
}
