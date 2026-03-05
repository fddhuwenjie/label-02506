package com.clinic.service;

import com.clinic.dto.DailyStatDTO;
import com.clinic.dto.MedicineStatDTO;
import com.clinic.dto.StatisticsDTO;
import com.clinic.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final RegistrationRepository registrationRepository;
    private final PaymentRepository paymentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;
    
    public StatisticsDTO getStatistics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        
        StatisticsDTO dto = new StatisticsDTO();
        dto.setTotalRegistrations(registrationRepository.countByDateRange(startDate, endDate));
        dto.setCompletedVisits(registrationRepository.countCompletedByDateRange(startDate, endDate));
        dto.setCancelledRegistrations(registrationRepository.countCancelledByDateRange(startDate, endDate));
        dto.setTotalPrescriptions(prescriptionRepository.countByDateRange(start, end));
        
        BigDecimal totalRevenue = paymentRepository.sumPaidAmountByDateRange(start, end);
        dto.setTotalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        
        BigDecimal regFee = paymentRepository.sumByFeeTypeAndDateRange(1, start, end);
        dto.setRegistrationFee(regFee != null ? regFee : BigDecimal.ZERO);
        
        BigDecimal medFee = paymentRepository.sumByFeeTypeAndDateRange(3, start, end);
        dto.setMedicineFee(medFee != null ? medFee : BigDecimal.ZERO);
        
        dto.setNewPatients(userRepository.countNewPatientsByDateRange(start, end));
        
        return dto;
    }
    
    public List<DailyStatDTO> getDailyStats(LocalDate startDate, LocalDate endDate) {
        List<DailyStatDTO> result = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            LocalDateTime dayStart = current.atStartOfDay();
            LocalDateTime dayEnd = current.plusDays(1).atStartOfDay();
            
            Long regs = registrationRepository.countByDate(current);
            Long visits = registrationRepository.countCompletedByDate(current);
            BigDecimal revenue = paymentRepository.sumPaidAmountByDateRange(dayStart, dayEnd);
            
            result.add(new DailyStatDTO(current, regs, visits, revenue != null ? revenue : BigDecimal.ZERO));
            current = current.plusDays(1);
        }
        return result;
    }
    
    public List<MedicineStatDTO> getTopMedicines(LocalDate startDate, LocalDate endDate, int limit) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        return prescriptionRepository.findTopMedicines(start, end, limit);
    }
}
