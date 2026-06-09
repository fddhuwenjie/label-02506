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

    /**
     * 通过悲观写锁加载指定医生、星期、状态的挂号规则。
     * 用于挂号创建流程中对“号源剩余量校验 + 排队号生成 + 插入挂号”进行原子化串行处理，
     * 避免并发挂号导致号源超卖与排队号重复。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RegistrationRule r WHERE r.doctor.id = ?1 AND r.weekDay = ?2 AND r.status = ?3")
    List<RegistrationRule> findByDoctorIdAndWeekDayAndStatusForUpdate(Long doctorId, Integer weekDay, Integer status);
}
