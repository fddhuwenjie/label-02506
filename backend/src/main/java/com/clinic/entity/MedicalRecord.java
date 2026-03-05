package com.clinic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "medical_record")
public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "record_no", nullable = false, unique = true, length = 50)
    private String recordNo;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private Registration registration;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;
    
    @Column(name = "visit_time", nullable = false)
    private LocalDateTime visitTime;
    
    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;
    
    @Column(name = "present_illness", columnDefinition = "TEXT")
    private String presentIllness;
    
    @Column(name = "physical_exam", columnDefinition = "TEXT")
    private String physicalExam;
    
    @Column(length = 500)
    private String diagnosis;
    
    @Column(name = "diagnosis_code", length = 50)
    private String diagnosisCode;
    
    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;
    
    @Column(name = "doctor_advice", columnDefinition = "TEXT")
    private String doctorAdvice;
    
    @Column(name = "is_follow_up")
    private Integer isFollowUp = 0;
    
    @Column(name = "follow_up_date")
    private LocalDate followUpDate;
    
    @Column(name = "follow_up_note", length = 255)
    private String followUpNote;
    
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
