package com.clinic.repository;

import com.clinic.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    Optional<Medicine> findByMedicineCode(String medicineCode);
    
    List<Medicine> findByStatus(Integer status);
    
    List<Medicine> findByMedicineNameContaining(String name);
    
    @Query("SELECT m FROM Medicine m WHERE m.stockQuantity <= m.warningQuantity AND m.status = 1")
    List<Medicine> findLowStockMedicines();
    
    boolean existsByMedicineCode(String medicineCode);
}
