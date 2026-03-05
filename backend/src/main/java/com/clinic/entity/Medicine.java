package com.clinic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "medicine")
public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "medicine_code", nullable = false, unique = true, length = 50)
    private String medicineCode;
    
    @Column(name = "medicine_name", nullable = false, length = 100)
    private String medicineName;
    
    @Column(name = "generic_name", length = 100)
    private String genericName;
    
    @Column(length = 100)
    private String specification;
    
    @Column(nullable = false, length = 20)
    private String unit;
    
    @Column(length = 200)
    private String manufacturer;
    
    @Column(length = 50)
    private String category;
    
    @Column(name = "dosage_form", length = 50)
    private String dosageForm;
    
    @Column(name = "purchase_price", precision = 10, scale = 2)
    private BigDecimal purchasePrice;
    
    @Column(name = "sell_price", precision = 10, scale = 2)
    private BigDecimal sellPrice;
    
    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;
    
    @Column(name = "warning_quantity")
    private Integer warningQuantity = 10;
    
    @Column(name = "shelf_life")
    private Integer shelfLife;
    
    @Column(name = "storage_condition", length = 100)
    private String storageCondition;
    
    @Column(name = "usage_method", columnDefinition = "TEXT")
    private String usageMethod;
    
    @Column(columnDefinition = "TEXT")
    private String contraindication;
    
    private Integer status = 1;
    
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
    
    public boolean isLowStock() {
        return stockQuantity != null && warningQuantity != null && stockQuantity <= warningQuantity;
    }
}
