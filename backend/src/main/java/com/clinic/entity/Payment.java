package com.clinic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "payment_no", nullable = false, unique = true, length = 50)
    private String paymentNo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id")
    private Registration registration;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;
    
    @Column(name = "fee_type", nullable = false)
    private Integer feeType; // 1-挂号费，2-诊疗费，3-药品费，4-其他
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    
    @Column(name = "payment_method")
    private Integer paymentMethod; // 1-现金，2-微信，3-支付宝，4-银行卡
    
    private Integer status = 0; // 0-待支付，1-已支付，2-部分支付，3-已退款
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;
    
    @Column(name = "pay_time")
    private LocalDateTime payTime;
    
    @Column(length = 255)
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
    
    public String getStatusText() {
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "部分支付";
            case 3 -> "已退款";
            default -> "未知";
        };
    }
    
    public String getFeeTypeText() {
        return switch (feeType) {
            case 1 -> "挂号费";
            case 2 -> "诊疗费";
            case 3 -> "药品费";
            case 4 -> "其他";
            default -> "未知";
        };
    }
}
