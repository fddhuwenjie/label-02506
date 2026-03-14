package com.clinic.controller;

import com.clinic.entity.Prescription;
import com.clinic.entity.MedicalRecord;
import com.clinic.security.CustomUserDetails;
import com.clinic.service.PrescriptionService;
import com.clinic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/doctor/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {
    private final PrescriptionService prescriptionService;
    private final UserService userService;
    
    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("pendingList", prescriptionService.findPendingAuditByDoctor(userDetails.getId()));
        model.addAttribute("allList", prescriptionService.findByDoctor(userDetails.getId()));
        return "doctor/prescription-list";
    }
    
    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal CustomUserDetails userDetails,
                         @PathVariable Long id, 
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Prescription prescription = prescriptionService.findById(id);
        if (prescription == null) {
            redirectAttributes.addFlashAttribute("error", "处方不存在");
            return "redirect:/doctor/prescriptions";
        }
        MedicalRecord record = prescription.getMedicalRecord();
        if (record == null || record.getDoctor() == null || !record.getDoctor().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权查看该处方");
            return "redirect:/doctor/prescriptions";
        }
        model.addAttribute("prescription", prescription);
        return "doctor/prescription-detail";
    }
    
    @PostMapping("/{id}/audit")
    public String audit(@PathVariable Long id, 
                       @AuthenticationPrincipal CustomUserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        Prescription prescription = prescriptionService.findById(id);
        if (prescription == null) {
            redirectAttributes.addFlashAttribute("error", "处方不存在");
            return "redirect:/doctor/prescriptions";
        }
        MedicalRecord record = prescription.getMedicalRecord();
        if (record == null || record.getDoctor() == null || !record.getDoctor().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权审核该处方");
            return "redirect:/doctor/prescriptions";
        }
        prescriptionService.auditPrescription(id, userService.findById(userDetails.getId()));
        redirectAttributes.addFlashAttribute("message", "处方审核通过");
        return "redirect:/doctor/prescriptions";
    }
    
    @PostMapping("/{id}/dispense")
    public String dispense(@AuthenticationPrincipal CustomUserDetails userDetails,
                           @PathVariable Long id, 
                           RedirectAttributes redirectAttributes) {
        Prescription prescription = prescriptionService.findById(id);
        if (prescription == null) {
            redirectAttributes.addFlashAttribute("error", "处方不存在");
            return "redirect:/doctor/prescriptions";
        }
        MedicalRecord record = prescription.getMedicalRecord();
        if (record == null || record.getDoctor() == null || !record.getDoctor().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权发药该处方");
            return "redirect:/doctor/prescriptions";
        }
        prescriptionService.dispensePrescription(id);
        redirectAttributes.addFlashAttribute("message", "发药成功");
        return "redirect:/doctor/prescriptions";
    }
    
    @GetMapping("/{id}/print")
    public String print(@AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable Long id, 
                        Model model,
                        RedirectAttributes redirectAttributes) {
        Prescription prescription = prescriptionService.findById(id);
        if (prescription == null) {
            redirectAttributes.addFlashAttribute("error", "处方不存在");
            return "redirect:/doctor/prescriptions";
        }
        MedicalRecord record = prescription.getMedicalRecord();
        if (record == null || record.getDoctor() == null || !record.getDoctor().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权打印该处方");
            return "redirect:/doctor/prescriptions";
        }
        model.addAttribute("prescription", prescription);
        return "doctor/prescription-print";
    }
}
