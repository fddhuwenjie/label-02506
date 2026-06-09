package com.clinic.repository;

import com.clinic.entity.RegistrationRule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRuleRepository extends JpaRepository<RegistrationRule, Long> {
    List<RegistrationRule> findByDoctorIdAndStatus(Long doctorId, Integer status);
    List<RegistrationRule> findByDoctorIdAndWeekDayAndStatus(Long doctorId, Integer weekDay, Integer status);
    List<RegistrationRule> findByDoctorId(Long doctorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RegistrationRule r WHERE r.doctor.id = :doctorId AND r.weekDay = :weekDay AND r.timePeriod = :timePeriod AND r.status = 1")
    Optional<RegistrationRule> findByDoctorAndWeekDayAndPeriodWithLock(@Param("doctorId") Long doctorId,
                                                                       @Param("weekDay") Integer weekDay,
                                                                       @Param("timePeriod") Integer timePeriod);
}
