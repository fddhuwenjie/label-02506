package com.clinic.service;

import com.clinic.entity.Medicine;
import com.clinic.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicineService {
    private final MedicineRepository medicineRepository;
    
    public List<Medicine> findAll() {
        return medicineRepository.findAll();
    }
    
    public List<Medicine> findActive() {
        return medicineRepository.findByStatus(1);
    }
    
    public Medicine findById(Long id) {
        return medicineRepository.findById(id).orElse(null);
    }
    
    public Medicine findByCode(String code) {
        return medicineRepository.findByMedicineCode(code).orElse(null);
    }
    
    public List<Medicine> search(String name) {
        return medicineRepository.findByMedicineNameContaining(name);
    }
    
    public List<Medicine> findLowStock() {
        return medicineRepository.findLowStockMedicines();
    }
    
    @Transactional
    public Medicine save(Medicine medicine) {
        if (medicine == null) {
            throw new IllegalArgumentException("药品信息不能为空");
        }
        if (medicine.getMedicineName() == null || medicine.getMedicineName().isBlank()) {
            throw new IllegalArgumentException("药品名称不能为空");
        }
        if (medicine.getMedicineCode() == null || medicine.getMedicineCode().isBlank()) {
            throw new IllegalArgumentException("药品编码不能为空");
        }
        return medicineRepository.save(medicine);
    }
    
    @Transactional
    public void updateStock(Long id, Integer quantity) {
        if (id == null) {
            throw new IllegalArgumentException("药品ID不能为空");
        }
        if (quantity == null) {
            throw new IllegalArgumentException("数量不能为空");
        }
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("药品不存在"));
        int currentStock = medicine.getStockQuantity() == null ? 0 : medicine.getStockQuantity();
        int nextStock = currentStock + quantity;
        if (nextStock < 0) {
            throw new IllegalArgumentException("药品库存不足（当前库存：" + currentStock + "，需要：" + Math.abs(quantity) + "）");
        }
        medicine.setStockQuantity(nextStock);
        medicineRepository.save(medicine);
    }
    
    @Transactional
    public void updateWarningQuantity(Long id, Integer quantity) {
        if (id == null) {
            throw new IllegalArgumentException("药品ID不能为空");
        }
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("预警数量不能为空且不能为负数");
        }
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("药品不存在"));
        medicine.setWarningQuantity(quantity);
        medicineRepository.save(medicine);
    }
    
    public boolean existsByCode(String code) {
        return medicineRepository.existsByMedicineCode(code);
    }
}
