package com.clinic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "registration")
public class Registration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "reg_no", nullable = false, unique = true, length = 50)
    private String regNo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;
    
    @Column(name = "reg_date", nullable = false)
    private LocalDate regDate;
    
    @Column(name = "time_period", nullable = false)
    private Integer timePeriod; // 1-上午，2-下午
    
    @Column(name = "queue_no", nullable = false)
    private Integer queueNo;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal fee;
    
    private Integer status = 0; // 0-待就诊，1-就诊中，2-已完成，3-已取消，4-爽约
    
    private Integer source = 0; // 0-线上预约，1-现场挂号
    
    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;
    
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
    
    public String getStatusText() {
        return switch (status) {
            case 0 -> "待就诊";
            case 1 -> "就诊中";
            case 2 -> "已完成";
            case 3 -> "已取消";
            case 4 -> "爽约";
            default -> "未知";
        };
    }
    
    public String getTimePeriodText() {
        return timePeriod == 1 ? "上午" : "下午";
    }
}
