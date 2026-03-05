package com.clinic.controller;

import com.alibaba.excel.EasyExcel;
import com.clinic.dto.DailyStatDTO;
import com.clinic.entity.PatientProfile;
import com.clinic.entity.User;
import com.clinic.excel.PatientExcel;
import com.clinic.excel.StatisticsExcel;
import com.clinic.service.StatisticsService;
import com.clinic.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/doctor/export")
@RequiredArgsConstructor
public class ExportController {
    private final UserService userService;
    private final StatisticsService statisticsService;
    
    @GetMapping("/patients")
    public void exportPatients(HttpServletResponse response) throws IOException {
        setExcelResponse(response, "病人档案");
        
        List<User> patients = userService.findPatients();
        List<PatientExcel> data = new ArrayList<>();
        
        for (User patient : patients) {
            PatientProfile profile = userService.getPatientProfile(patient.getId());
            PatientExcel excel = new PatientExcel();
            excel.setMedicalCardNo(profile != null ? profile.getMedicalCardNo() : "-");
            excel.setRealName(patient.getRealName());
            excel.setGender(profile != null && profile.getGender() != null ? 
                (profile.getGender() == 1 ? "男" : "女") : "-");
            excel.setBirthDate(profile != null && profile.getBirthDate() != null ? 
                profile.getBirthDate().toString() : "-");
            excel.setPhone(patient.getPhone() != null ? patient.getPhone() : "-");
            excel.setBloodType(profile != null && profile.getBloodType() != null ? 
                profile.getBloodType() + "型" : "-");
            excel.setAddress(profile != null && profile.getAddress() != null ? 
                profile.getAddress() : "-");
            excel.setAllergyHistory(profile != null && profile.getAllergyHistory() != null ? 
                profile.getAllergyHistory() : "无");
            excel.setCreateTime(patient.getCreateTime() != null ? 
                patient.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-");
            data.add(excel);
        }
        
        EasyExcel.write(response.getOutputStream(), PatientExcel.class)
                .sheet("病人档案")
                .doWrite(data);
    }
    
    @GetMapping("/statistics")
    public void exportStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) throws IOException {
        
        setExcelResponse(response, "统计报表_" + startDate + "_" + endDate);
        
        List<DailyStatDTO> dailyStats = statisticsService.getDailyStats(startDate, endDate);
        List<StatisticsExcel> data = new ArrayList<>();
        
        for (DailyStatDTO stat : dailyStats) {
            StatisticsExcel excel = new StatisticsExcel();
            excel.setDate(stat.getDate().toString());
            excel.setRegistrations(stat.getRegistrations());
            excel.setVisits(stat.getVisits());
            excel.setRevenue(stat.getRevenue().toString());
            data.add(excel);
        }
        
        EasyExcel.write(response.getOutputStream(), StatisticsExcel.class)
                .sheet("每日统计")
                .doWrite(data);
    }
    
    private void setExcelResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
    }
}
