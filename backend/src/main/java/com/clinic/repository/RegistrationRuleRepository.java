package com.clinic.repository;

import com.clinic.entity.RegistrationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RegistrationRuleRepository extends JpaRepository<RegistrationRule, Long> {
    List<RegistrationRule> findByDoctorIdAndStatus(Long doctorId, Integer status);
    List<RegistrationRule> findByDoctorIdAndWeekDayAndStatus(Long doctorId, Integer weekDay, Integer status);
}
