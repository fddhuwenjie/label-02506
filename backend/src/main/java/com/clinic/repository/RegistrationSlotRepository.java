package com.clinic.repository;

import com.clinic.entity.RegistrationSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RegistrationSlotRepository extends JpaRepository<RegistrationSlot, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM RegistrationSlot s WHERE s.doctorId = ?1 AND s.regDate = ?2 AND s.timePeriod = ?3")
    Optional<RegistrationSlot> findByDoctorIdAndRegDateAndTimePeriodWithLock(Long doctorId, LocalDate regDate, Integer timePeriod);
    
    Optional<RegistrationSlot> findByDoctorIdAndRegDateAndTimePeriod(Long doctorId, LocalDate regDate, Integer timePeriod);
}
