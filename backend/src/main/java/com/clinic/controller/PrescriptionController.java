package com.clinic.controller;

import com.clinic.entity.Prescription;
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
    public String list(Model model) {
        model.addAttribute("pendingList", prescriptionService.findPendingAudit());
        model.addAttribute("allList", prescriptionService.findAll());
        return "doctor/prescription-list";
    }
    
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Prescription prescription = prescriptionService.findById(id);
        model.addAttribute("prescription", prescription);
        return "doctor/prescription-detail";
    }
    
    @PostMapping("/{id}/audit")
    public String audit(@PathVariable Long id, 
                       @AuthenticationPrincipal CustomUserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        prescriptionService.auditPrescription(id, userService.findById(userDetails.getId()));
        redirectAttributes.addFlashAttribute("message", "处方审核通过");
        return "redirect:/doctor/prescriptions";
    }
    
    @PostMapping("/{id}/dispense")
    public String dispense(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        prescriptionService.dispensePrescription(id);
        redirectAttributes.addFlashAttribute("message", "发药成功");
        return "redirect:/doctor/prescriptions";
    }
    
    @GetMapping("/{id}/print")
    public String print(@PathVariable Long id, Model model) {
        model.addAttribute("prescription", prescriptionService.findById(id));
        return "doctor/prescription-print";
    }
}
