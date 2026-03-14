package com.clinic.controller;

import com.clinic.entity.*;
import com.clinic.security.CustomUserDetails;
import com.clinic.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/patient")
@RequiredArgsConstructor
public class PatientController {
    private final UserService userService;
    private final RegistrationService registrationService;
    private final MedicalRecordService recordService;
    private final PrescriptionService prescriptionService;
    private final PaymentService paymentService;
    
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        Long patientId = userDetails.getId();
        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("profile", userService.getPatientProfile(patientId));
        model.addAttribute("upcomingRegistrations", registrationService.findByPatient(patientId)
            .stream().filter(r -> r.getStatus() == 0).toList());
        model.addAttribute("followUps", recordService.findUpcomingFollowUps(patientId));
        model.addAttribute("unpaidPayments", paymentService.findUnpaidByPatient(patientId));
        return "patient/dashboard";
    }
    
    // 个人档案
    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("profile", userService.getPatientProfile(userDetails.getId()));
        return "patient/profile";
    }
    
    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @ModelAttribute PatientProfile profile,
                               RedirectAttributes redirectAttributes) {
        PatientProfile existing = userService.getPatientProfile(userDetails.getId());
        existing.setGender(profile.getGender());
        existing.setBirthDate(profile.getBirthDate());
        existing.setAddress(profile.getAddress());
        existing.setEmergencyContact(profile.getEmergencyContact());
        existing.setEmergencyPhone(profile.getEmergencyPhone());
        existing.setBloodType(profile.getBloodType());
        existing.setAllergyHistory(profile.getAllergyHistory());
        userService.updatePatientProfile(existing);
        redirectAttributes.addFlashAttribute("message", "档案更新成功");
        return "redirect:/patient/profile";
    }
    
    // 在线挂号
    @GetMapping("/registration")
    public String registrationPage(Model model) {
        model.addAttribute("doctors", userService.findDoctors());
        return "patient/registration";
    }
    
    @GetMapping("/registration/schedule")
    @ResponseBody
    public List<RegistrationRule> getDoctorSchedule(@RequestParam Long doctorId) {
        return registrationService.getDoctorRules(doctorId);
    }
    
    @PostMapping("/registration/create")
    public String createRegistration(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @RequestParam Long doctorId,
                                    @RequestParam String date,
                                    @RequestParam Integer timePeriod,
                                    RedirectAttributes redirectAttributes) {
        LocalDate regDate = LocalDate.parse(date);
        
        if (!registrationService.hasSchedule(doctorId, regDate, timePeriod)) {
            String periodText = timePeriod == 1 ? "上午" : "下午";
            String weekDayText = getWeekDayText(regDate.getDayOfWeek().getValue());
            redirectAttributes.addFlashAttribute("error", 
                "该医生" + weekDayText + periodText + "没有出诊安排，请选择其他时间");
            return "redirect:/patient/registration";
        }
        
        if (!registrationService.canRegister(doctorId, regDate, timePeriod)) {
            redirectAttributes.addFlashAttribute("error", "该时段已约满");
            return "redirect:/patient/registration";
        }
        
        User patient = userService.findById(userDetails.getId());
        User doctor = userService.findById(doctorId);
        if (registrationService.hasDuplicateRegistration(patient.getId(), doctor.getId(), regDate, timePeriod)) {
            redirectAttributes.addFlashAttribute("error", "重复挂号：该医生该时段您已挂号，请勿重复提交");
            return "redirect:/patient/registration";
        }
        registrationService.createRegistration(patient, doctor, regDate, timePeriod, 0);
        
        redirectAttributes.addFlashAttribute("message", "挂号成功");
        return "redirect:/patient/my-registrations";
    }
    
    @GetMapping("/my-registrations")
    public String myRegistrations(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("registrations", registrationService.findByPatient(userDetails.getId()));
        return "patient/my-registrations";
    }
    
    @PostMapping("/registration/{id}/cancel")
    public String cancelRegistration(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long id, 
                                     RedirectAttributes redirectAttributes) {
        Registration reg = registrationService.findById(id);
        if (reg == null) {
            redirectAttributes.addFlashAttribute("error", "挂号记录不存在");
            return "redirect:/patient/my-registrations";
        }
        if (!reg.getPatient().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权取消该挂号");
            return "redirect:/patient/my-registrations";
        }
        if (reg.getStatus() != 0) {
            redirectAttributes.addFlashAttribute("error", "该挂号状态不允许取消");
            return "redirect:/patient/my-registrations";
        }
        registrationService.cancelRegistration(id, "病人取消");
        redirectAttributes.addFlashAttribute("message", "挂号已取消");
        return "redirect:/patient/my-registrations";
    }
    
    // 病历查看
    @GetMapping("/records")
    public String myRecords(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("records", recordService.findByPatient(userDetails.getId()));
        return "patient/records";
    }
    
    @GetMapping("/records/{id}")
    public String recordDetail(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @PathVariable Long id, 
                               Model model,
                               RedirectAttributes redirectAttributes) {
        MedicalRecord record = recordService.findById(id);
        if (record == null) {
            redirectAttributes.addFlashAttribute("error", "病历不存在");
            return "redirect:/patient/records";
        }
        if (!record.getPatient().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权查看该病历");
            return "redirect:/patient/records";
        }
        model.addAttribute("record", record);
        return "patient/record-detail";
    }
    
    // 处方查看
    @GetMapping("/prescriptions")
    public String myPrescriptions(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("prescriptions", prescriptionService.findByPatient(userDetails.getId()));
        return "patient/prescriptions";
    }
    
    @GetMapping("/prescriptions/{id}")
    public String prescriptionDetail(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long id, 
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        Prescription prescription = prescriptionService.findById(id);
        if (prescription == null) {
            redirectAttributes.addFlashAttribute("error", "处方不存在");
            return "redirect:/patient/prescriptions";
        }
        if (!prescription.getPatient().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权查看该处方");
            return "redirect:/patient/prescriptions";
        }
        model.addAttribute("prescription", prescription);
        return "patient/prescription-detail";
    }
    
    @GetMapping("/prescriptions/{id}/print")
    public String prescriptionPrint(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @PathVariable Long id, 
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        Prescription prescription = prescriptionService.findById(id);
        if (prescription == null) {
            redirectAttributes.addFlashAttribute("error", "处方不存在");
            return "redirect:/patient/prescriptions";
        }
        if (!prescription.getPatient().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权打印该处方");
            return "redirect:/patient/prescriptions";
        }
        model.addAttribute("prescription", prescription);
        return "patient/prescription-print";
    }
    
    // 缴费记录
    @GetMapping("/payments")
    public String myPayments(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("payments", paymentService.findByPatient(userDetails.getId()));
        return "patient/payments";
    }

    @PostMapping("/payments/{id}/pay")
    public String payOnline(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @PathVariable Long id,
                            @RequestParam BigDecimal paidAmount,
                            @RequestParam Integer paymentMethod,
                            RedirectAttributes redirectAttributes) {
        Payment payment = paymentService.findById(id);
        if (payment == null) {
            redirectAttributes.addFlashAttribute("error", "缴费单不存在");
            return "redirect:/patient/payments";
        }
        Long paymentPatientId = payment.getPatient() != null ? payment.getPatient().getId() : null;
        if (paymentPatientId == null || !paymentPatientId.equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权操作该缴费单");
            return "redirect:/patient/payments";
        }
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("error", "缴费金额必须大于 0");
            return "redirect:/patient/payments";
        }
        try {
            User operator = userService.findById(userDetails.getId());
            paymentService.pay(id, paidAmount, paymentMethod, operator);
            redirectAttributes.addFlashAttribute("message", "缴费成功");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "缴费失败，请稍后重试");
        }
        return "redirect:/patient/payments";
    }
    
    private String getWeekDayText(int weekDay) {
        return switch (weekDay) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "";
        };
    }
}
