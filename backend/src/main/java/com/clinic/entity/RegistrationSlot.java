package com.clinic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "registration_slot", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"doctor_id", "reg_date", "time_period"})
})
public class RegistrationSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;
    
    @Column(name = "reg_date", nullable = false)
    private LocalDate regDate;
    
    @Column(name = "time_period", nullable = false)
    private Integer timePeriod;
    
    @Column(name = "current_count", nullable = false)
    private Integer currentCount = 0;
    
    @Column(name = "max_count", nullable = false)
    private Integer maxCount;
    
    @Column(name = "current_queue_no", nullable = false)
    private Integer currentQueueNo = 0;
    
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
