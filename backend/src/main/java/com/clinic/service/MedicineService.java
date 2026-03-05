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
        return medicineRepository.save(medicine);
    }
    
    @Transactional
    public void updateStock(Long id, Integer quantity) {
        Medicine medicine = medicineRepository.findById(id).orElseThrow();
        int currentStock = medicine.getStockQuantity() == null ? 0 : medicine.getStockQuantity();
        int nextStock = currentStock + quantity;
        if (nextStock < 0) {
            throw new IllegalArgumentException("药品库存不足，无法发药");
        }
        medicine.setStockQuantity(nextStock);
        medicineRepository.save(medicine);
    }
    
    @Transactional
    public void updateWarningQuantity(Long id, Integer quantity) {
        Medicine medicine = medicineRepository.findById(id).orElseThrow();
        medicine.setWarningQuantity(quantity);
        medicineRepository.save(medicine);
    }
    
    public boolean existsByCode(String code) {
        return medicineRepository.existsByMedicineCode(code);
    }
}
