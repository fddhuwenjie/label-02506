package com.clinic.controller;

import com.clinic.entity.Medicine;
import com.clinic.service.*;
import com.clinic.dto.StatisticsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/doctor/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService statisticsService;
    private final MedicineService medicineService;
    
    @GetMapping
    public String statisticsPage(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            Model model) {
        
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();
        
        // 确保结束日期不早于开始日期
        if (endDate.isBefore(startDate)) {
            endDate = startDate;
        }
        // 限制日期范围不超过一年
        if (startDate.plusYears(1).isBefore(endDate)) {
            startDate = endDate.minusYears(1);
        }
        
        StatisticsDTO stats = statisticsService.getStatistics(startDate, endDate);
        model.addAttribute("stats", stats);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("dailyStats", statisticsService.getDailyStats(startDate, endDate));
        model.addAttribute("topMedicines", statisticsService.getTopMedicines(startDate, endDate, 10));
        
        return "doctor/statistics";
    }
    
    @GetMapping("/medicine-warning")
    public String medicineWarning(Model model) {
        List<Medicine> lowStock = medicineService.findLowStock();
        model.addAttribute("lowStockMedicines", lowStock);
        model.addAttribute("allMedicines", medicineService.findActive());
        return "doctor/medicine-warning";
    }
}
