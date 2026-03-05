package com.clinic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "prescription")
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "prescription_no", nullable = false, unique = true, length = 50)
    private String prescriptionNo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id", nullable = false)
    private MedicalRecord medicalRecord;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;
    
    @Column(length = 500)
    private String diagnosis;
    
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    private Integer status = 0; // 0-待审核，1-已审核，2-已发药，3-已作废
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_doctor_id")
    private User auditDoctor;
    
    @Column(name = "audit_time")
    private LocalDateTime auditTime;
    
    @Column(name = "dispense_time")
    private LocalDateTime dispenseTime;
    
    @Column(columnDefinition = "TEXT")
    private String remark;
    
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PrescriptionDetail> details;
    
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
            case 0 -> "待审核";
            case 1 -> "已审核";
            case 2 -> "已发药";
            case 3 -> "已作废";
            default -> "未知";
        };
    }
}
