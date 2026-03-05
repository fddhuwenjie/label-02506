package com.clinic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "patient_profile")
public class PatientProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(name = "medical_card_no", unique = true, length = 50)
    private String medicalCardNo;
    
    private Integer gender;
    
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    @Column(name = "id_card", length = 18)
    private String idCard;
    
    @Column(length = 255)
    private String address;
    
    @Column(name = "emergency_contact", length = 50)
    private String emergencyContact;
    
    @Column(name = "emergency_phone", length = 20)
    private String emergencyPhone;
    
    @Column(name = "blood_type", length = 10)
    private String bloodType;
    
    @Column(name = "allergy_history", columnDefinition = "TEXT")
    private String allergyHistory;
    
    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;
    
    @Column(columnDefinition = "TEXT")
    private String remark;
    
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
