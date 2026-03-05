package com.clinic.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class MedicineStatDTO {
    private Long medicineId;
    private String medicineName;
    private Long totalQuantity;
    private BigDecimal totalAmount;
    
    public MedicineStatDTO(Long medicineId, String medicineName, Long totalQuantity, BigDecimal totalAmount) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.totalQuantity = totalQuantity;
        this.totalAmount = totalAmount;
    }
}
