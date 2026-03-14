package com.clinic.controller;

import com.clinic.dto.ApiResponse;
import com.clinic.entity.*;
import com.clinic.security.CustomUserDetails;
import com.clinic.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/doctor")
@RequiredArgsConstructor
@Slf4j
public class DoctorController {
    private final UserService userService;
    private final RegistrationService registrationService;
    private final MedicineService medicineService;
    private final MedicalRecordService recordService;
    private final PaymentService paymentService;
    private final PrescriptionService prescriptionService;
    
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("todayRegistrations", registrationService.countTodayRegistrations());
        model.addAttribute("todayRevenue", paymentService.getTodayRevenue());
        model.addAttribute("lowStockMedicines", medicineService.findLowStock());
        model.addAttribute("todayFollowUps", recordService.findTodayFollowUps());
        
        List<Registration> todayList = registrationService.findByDoctorAndDate(
            userDetails.getId(), LocalDate.now());
        model.addAttribute("todayPatients", todayList);
        return "doctor/dashboard";
    }
    
    // 病人档案管理
    @GetMapping("/patients")
    public String patientList(Model model) {
        model.addAttribute("patients", userService.findPatients());
        return "doctor/patient-list";
    }
    
    @GetMapping("/patients/{id}")
    public String patientDetail(@PathVariable Long id, Model model) {
        User patient = userService.findById(id);
        PatientProfile profile = userService.getPatientProfile(id);
        List<MedicalRecord> records = recordService.findByPatient(id);
        model.addAttribute("patient", patient);
        model.addAttribute("profile", profile);
        model.addAttribute("records", records);
        return "doctor/patient-detail";
    }
    
    // 挂号管理
    @GetMapping("/registrations")
    public String registrationList(@RequestParam(required = false) String date, Model model) {
        LocalDate queryDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        model.addAttribute("registrations", registrationService.findByDate(queryDate));
        model.addAttribute("queryDate", queryDate);
        return "doctor/registration-list";
    }

    @GetMapping("/registrations/manual")
    public String manualRegistrationPage(Model model) {
        model.addAttribute("patients", userService.findPatients());
        model.addAttribute("today", LocalDate.now());
        return "doctor/manual-registration";
    }

    @PostMapping("/registrations/manual/create")
    public String createManualRegistration(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @RequestParam Long patientId,
                                           @RequestParam String date,
                                           @RequestParam Integer timePeriod,
                                           RedirectAttributes redirectAttributes) {
        LocalDate regDate = LocalDate.parse(date);
        if (!registrationService.hasSchedule(userDetails.getId(), regDate, timePeriod)) {
            String periodText = timePeriod == 1 ? "上午" : "下午";
            String weekDayText = getWeekDayText(regDate.getDayOfWeek().getValue());
            redirectAttributes.addFlashAttribute("error", 
                "您在" + weekDayText + periodText + "没有出诊安排，请先在「挂号规则」中配置出诊时间");
            return "redirect:/doctor/registrations/manual";
        }
        if (!registrationService.canRegister(userDetails.getId(), regDate, timePeriod)) {
            redirectAttributes.addFlashAttribute("error", "该时段已约满");
            return "redirect:/doctor/registrations/manual";
        }
        User patient = userService.findById(patientId);
        if (patient == null) {
            redirectAttributes.addFlashAttribute("error", "病人不存在");
            return "redirect:/doctor/registrations/manual";
        }
        User doctor = userService.findById(userDetails.getId());
        if (registrationService.hasDuplicateRegistration(patient.getId(), doctor.getId(), regDate, timePeriod)) {
            redirectAttributes.addFlashAttribute("error", "重复挂号：该病人该时段已挂号");
            return "redirect:/doctor/registrations/manual";
        }
        registrationService.createRegistration(patient, doctor, regDate, timePeriod, 1);
        redirectAttributes.addFlashAttribute("message", "现场挂号成功");
        return "redirect:/doctor/registrations";
    }
    
    @PostMapping("/registrations/{id}/call")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> callPatient(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                          @PathVariable Long id) {
        Registration reg = registrationService.findById(id);
        if (reg == null) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("挂号记录不存在"));
        }
        if (!reg.getDoctor().getId().equals(userDetails.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("无权操作该挂号"));
        }
        if (reg.getStatus() != 0) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("只有待就诊状态的挂号才能叫号"));
        }
        try {
            registrationService.callPatient(id);
            return ResponseEntity.ok(ApiResponse.success("已叫号"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("叫号失败，请稍后重试"));
        }
    }
    
    @PostMapping("/registrations/{id}/complete")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> completeVisit(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                            @PathVariable Long id) {
        Registration reg = registrationService.findById(id);
        if (reg == null) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("挂号记录不存在"));
        }
        if (!reg.getDoctor().getId().equals(userDetails.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("无权操作该挂号"));
        }
        if (reg.getStatus() != 1) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("只有就诊中状态的挂号才能完诊"));
        }
        try {
            registrationService.completeVisit(id);
            return ResponseEntity.ok(ApiResponse.success("就诊已完成"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("完诊失败，请稍后重试"));
        }
    }

    // 费用管理
    @GetMapping("/payments")
    public String paymentManagement(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    @RequestParam(required = false) Long registrationId, 
                                    Model model) {
        model.addAttribute("allPayments", paymentService.findByDoctor(userDetails.getId()));
        model.addAttribute("registrationId", registrationId);
        if (registrationId != null) {
            Registration registration = registrationService.findById(registrationId);
            if (registration != null && registration.getDoctor().getId().equals(userDetails.getId())) {
                model.addAttribute("selectedRegistration", registration);
                model.addAttribute("registrationPayments", paymentService.findByRegistration(registrationId));
            }
        }
        return "doctor/payment-management";
    }

    @GetMapping("/payments/registrations-for-calc")
    @ResponseBody
    public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> getRegistrationsForCalc(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        List<Registration> registrations = registrationService.findByDoctorAndDateRange(userDetails.getId(), weekAgo, today);
        List<java.util.Map<String, Object>> result = registrations.stream().map(reg -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", reg.getId());
            map.put("patientName", reg.getPatient().getRealName());
            map.put("date", reg.getRegDate().toString());
            map.put("timePeriod", reg.getTimePeriodText());
            map.put("status", reg.getStatusText());
            return map;
        }).toList();
        return ResponseEntity.ok(ApiResponse.success("获取成功", result));
    }

    @PostMapping("/payments/calculate")
    public String calculatePayment(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   @RequestParam Long registrationId,
                                   @RequestParam Integer feeType,
                                   @RequestParam BigDecimal totalAmount,
                                   @RequestParam(required = false) String remark,
                                   RedirectAttributes redirectAttributes) {
        Registration registration = registrationService.findById(registrationId);
        if (registration == null) {
            redirectAttributes.addFlashAttribute("error", "挂号记录不存在");
            return "redirect:/doctor/payments";
        }
        if (!registration.getDoctor().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权操作该挂号的费用");
            return "redirect:/doctor/payments";
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("error", "费用金额必须大于 0");
            return "redirect:/doctor/payments?registrationId=" + registrationId;
        }
        User operator = userService.findById(userDetails.getId());
        paymentService.createPayment(registration.getPatient(), registration, feeType, totalAmount, remark, operator);
        redirectAttributes.addFlashAttribute("message", "费用核算成功，已生成待缴费记录");
        return "redirect:/doctor/payments?registrationId=" + registrationId;
    }

    @PostMapping("/payments/{id}/pay")
    public String recordPayment(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @PathVariable Long id,
                                @RequestParam BigDecimal paidAmount,
                                @RequestParam Integer paymentMethod,
                                RedirectAttributes redirectAttributes) {
        Payment payment = paymentService.findById(id);
        if (payment == null) {
            redirectAttributes.addFlashAttribute("error", "缴费单不存在");
            return "redirect:/doctor/payments";
        }
        Registration reg = payment.getRegistration();
        if (reg != null && !reg.getDoctor().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权操作该缴费单");
            return "redirect:/doctor/payments";
        }
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("error", "缴费金额必须大于 0");
            return "redirect:/doctor/payments";
        }
        User operator = userService.findById(userDetails.getId());
        try {
            paymentService.pay(id, paidAmount, paymentMethod, operator);
            redirectAttributes.addFlashAttribute("message", "缴费录入成功");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "缴费录入失败，请稍后重试");
        }
        return "redirect:/doctor/payments";
    }

    @PostMapping("/payments/{id}/arrears")
    public String registerArrears(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @PathVariable Long id,
                                  @RequestParam String arrearsRemark,
                                  RedirectAttributes redirectAttributes) {
        Payment payment = paymentService.findById(id);
        if (payment == null) {
            redirectAttributes.addFlashAttribute("error", "缴费单不存在");
            return "redirect:/doctor/payments";
        }
        Registration reg = payment.getRegistration();
        if (reg != null && !reg.getDoctor().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权操作该缴费单");
            return "redirect:/doctor/payments";
        }
        if (arrearsRemark == null || arrearsRemark.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "请填写欠费说明");
            return "redirect:/doctor/payments";
        }
        User operator = userService.findById(userDetails.getId());
        try {
            paymentService.registerArrears(id, arrearsRemark, operator);
            redirectAttributes.addFlashAttribute("message", "欠费登记成功");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "欠费登记失败，请稍后重试");
        }
        return "redirect:/doctor/payments";
    }
    
    // 药品管理
    @GetMapping("/medicines")
    public String medicineList(Model model) {
        model.addAttribute("medicines", medicineService.findAll());
        model.addAttribute("lowStockCount", medicineService.findLowStock().size());
        return "doctor/medicine-list";
    }
    
    @GetMapping("/medicines/add")
    public String addMedicinePage() {
        return "doctor/medicine-form";
    }
    
    @PostMapping("/medicines/save")
    public String saveMedicine(@ModelAttribute Medicine medicine) {
        medicineService.save(medicine);
        return "redirect:/doctor/medicines";
    }
    
    @GetMapping("/medicines/{id}/edit")
    public String editMedicinePage(@PathVariable Long id, Model model) {
        model.addAttribute("medicine", medicineService.findById(id));
        return "doctor/medicine-form";
    }
    
    @PostMapping("/medicines/{id}/warning")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> updateWarningQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        try {
            medicineService.updateWarningQuantity(id, quantity);
            return ResponseEntity.ok(ApiResponse.success("预警库存已更新"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("更新失败，请确认药品有效"));
        }
    }
    
    // 就诊接诊
    @GetMapping("/visit/{registrationId}")
    public String visitPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @PathVariable Long registrationId, 
                            Model model,
                            RedirectAttributes redirectAttributes) {
        Registration reg = registrationService.findById(registrationId);
        if (reg == null) {
            redirectAttributes.addFlashAttribute("error", "挂号记录不存在");
            return "redirect:/doctor/registrations";
        }
        if (!reg.getDoctor().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权接诊该挂号");
            return "redirect:/doctor/registrations";
        }
        MedicalRecord record = recordService.findByRegistration(registrationId);
        PatientProfile profile = userService.getPatientProfile(reg.getPatient().getId());
        Prescription prescription = null;
        if (record != null) {
            prescription = prescriptionService.findByMedicalRecord(record.getId());
        }
        
        model.addAttribute("registration", reg);
        model.addAttribute("record", record);
        model.addAttribute("profile", profile);
        model.addAttribute("medicines", medicineService.findActive());
        model.addAttribute("prescription", prescription);
        return "doctor/visit";
    }

    @PostMapping("/visit/{registrationId}/prescription")
    public String createPrescription(@PathVariable Long registrationId,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestParam Long medicineId,
                                     @RequestParam Integer quantity,
                                     @RequestParam(required = false) String dosage,
                                     @RequestParam(required = false) String frequency,
                                     RedirectAttributes redirectAttributes) {
        Registration reg = registrationService.findById(registrationId);
        if (reg == null) {
            redirectAttributes.addFlashAttribute("error", "挂号记录不存在");
            return "redirect:/doctor/registrations";
        }
        if (!reg.getDoctor().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权操作该挂号");
            return "redirect:/doctor/registrations";
        }
        if (quantity == null || quantity <= 0) {
            redirectAttributes.addFlashAttribute("error", "数量必须大于 0");
            return "redirect:/doctor/visit/" + registrationId;
        }
        Medicine medicine = medicineService.findById(medicineId);
        if (medicine == null) {
            redirectAttributes.addFlashAttribute("error", "药品不存在");
            return "redirect:/doctor/visit/" + registrationId;
        }
        if (medicine.getStockQuantity() == null || medicine.getStockQuantity() < quantity) {
            redirectAttributes.addFlashAttribute("error", "药品库存不足");
            return "redirect:/doctor/visit/" + registrationId;
        }

        MedicalRecord record = recordService.findByRegistration(registrationId);
        if (record == null) {
            record = recordService.createRecord(reg);
        }
        Prescription existed = prescriptionService.findByMedicalRecord(record.getId());
        if (existed != null) {
            redirectAttributes.addFlashAttribute("message", "该就诊已存在处方，无需重复创建");
            return "redirect:/doctor/visit/" + registrationId;
        }

        PrescriptionDetail detail = new PrescriptionDetail();
        detail.setMedicine(medicine);
        detail.setMedicineName(medicine.getMedicineName());
        detail.setSpecification(medicine.getSpecification());
        detail.setUnit(medicine.getUnit() == null ? "份" : medicine.getUnit());
        detail.setUnitPrice(medicine.getSellPrice());
        detail.setQuantity(quantity);
        detail.setDosage((dosage == null || dosage.isBlank()) ? "遵医嘱" : dosage);
        detail.setFrequency((frequency == null || frequency.isBlank()) ? "每日2次" : frequency);
        detail.setAdministration("口服");

        User operator = userService.findById(userDetails.getId());
        Prescription prescription = prescriptionService.createPrescription(record, List.of(detail));
        prescriptionService.auditPrescription(prescription.getId(), operator);
        prescriptionService.dispensePrescription(prescription.getId());
        paymentService.createPrescriptionPaymentIfAbsent(prescription.getId(), reg, operator);
        redirectAttributes.addFlashAttribute("message", "处方已创建并发药，已生成缴费单（处方号：" + prescription.getPrescriptionNo() + "）");
        return "redirect:/doctor/prescriptions/" + prescription.getId();
    }
    
    @PostMapping("/visit/{registrationId}/start")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> startVisit(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @PathVariable Long registrationId) {
        Registration reg = registrationService.findById(registrationId);
        if (reg == null) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("挂号记录不存在"));
        }
        if (!reg.getDoctor().getId().equals(userDetails.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("无权操作该挂号"));
        }
        if (reg.getStatus() != 0 && reg.getStatus() != 1) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("该挂号状态不允许开始接诊"));
        }
        try {
            registrationService.startVisit(registrationId);

            MedicalRecord record = recordService.findByRegistration(registrationId);
            if (record == null) {
                recordService.createRecord(reg);
            }
            return ResponseEntity.ok(ApiResponse.success("已开始接诊，请录入病历"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("开始接诊失败，请稍后重试"));
        }
    }
    
    @PostMapping("/visit/{registrationId}/save")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> saveRecord(@PathVariable Long registrationId,
                                                         @AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @RequestParam(required = false) String chiefComplaint,
                                                         @RequestParam(required = false) String presentIllness,
                                                         @RequestParam(required = false) String physicalExam,
                                                         @RequestParam(required = false) String diagnosis,
                                                         @RequestParam(required = false) String treatmentPlan,
                                                         @RequestParam(required = false) Integer isFollowUp,
                                                         @RequestParam(required = false) String followUpDate,
                                                         @RequestParam(required = false) Long medicineId,
                                                         @RequestParam(required = false) Integer quantity,
                                                         @RequestParam(required = false) String dosage,
                                                         @RequestParam(required = false) String frequency) {
        Registration reg = registrationService.findById(registrationId);
        if (reg == null) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("挂号记录不存在"));
        }
        if (!reg.getDoctor().getId().equals(userDetails.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("无权操作该挂号"));
        }
        if (chiefComplaint == null || chiefComplaint.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("请填写主诉"));
        }
        if (diagnosis == null || diagnosis.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("请填写诊断结果"));
        }
        if (treatmentPlan == null || treatmentPlan.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("请填写治疗方案"));
        }

        try {
            MedicalRecord record = recordService.findByRegistration(registrationId);
            if (record == null) {
                record = recordService.createRecord(reg);
            }
            record.setChiefComplaint(chiefComplaint);
            record.setPresentIllness(presentIllness);
            record.setPhysicalExam(physicalExam);
            record.setDiagnosis(diagnosis);
            record.setTreatmentPlan(treatmentPlan);
            record.setIsFollowUp(isFollowUp != null ? isFollowUp : 0);
            if (followUpDate != null && !followUpDate.isEmpty()) {
                record.setFollowUpDate(LocalDate.parse(followUpDate));
            }
            recordService.save(record);

            String successMessage = "病历保存成功，就诊已完成";
            User operator = userService.findById(userDetails.getId());
            try {
                Prescription existed = prescriptionService.findByMedicalRecord(record.getId());
                if (existed == null) {
                    if (medicineId != null && quantity != null && quantity > 0) {
                        Medicine medicine = medicineService.findById(medicineId);
                        if (medicine != null && medicine.getStockQuantity() != null && medicine.getStockQuantity() >= quantity) {
                            PrescriptionDetail detail = new PrescriptionDetail();
                            detail.setMedicine(medicine);
                            detail.setMedicineName(medicine.getMedicineName());
                            detail.setSpecification(medicine.getSpecification());
                            detail.setUnit(medicine.getUnit() == null ? "份" : medicine.getUnit());
                            detail.setUnitPrice(medicine.getSellPrice());
                            detail.setQuantity(quantity);
                            detail.setDosage((dosage == null || dosage.isBlank()) ? "遵医嘱" : dosage);
                            detail.setFrequency((frequency == null || frequency.isBlank()) ? "每日2次" : frequency);
                            detail.setAdministration("口服");

                            Prescription prescription = prescriptionService.createPrescription(record, List.of(detail));
                            prescriptionService.auditPrescription(prescription.getId(), operator);
                            prescriptionService.dispensePrescription(prescription.getId());
                            paymentService.createPrescriptionPaymentIfAbsent(prescription.getId(), reg, operator);
                            successMessage = "病历和处方已保存，已发药并生成缴费单";
                        } else {
                            successMessage = "病历已保存，药品库存不足，未自动创建处方";
                        }
                    } else {
                        successMessage = "病历已保存，未填写药品信息，未自动创建处方";
                    }
                } else {
                    if (existed.getStatus() == null || existed.getStatus() < 1) {
                        prescriptionService.auditPrescription(existed.getId(), operator);
                    }
                    if (existed.getStatus() == null || existed.getStatus() < 2) {
                        prescriptionService.dispensePrescription(existed.getId());
                    }
                    paymentService.createPrescriptionPaymentIfAbsent(existed.getId(), reg, operator);
                    successMessage = "病历已保存，已补齐处方发药与缴费单";
                }
            } catch (Exception prescriptionEx) {
                log.error("保存病历后处理处方失败 registrationId={}", registrationId, prescriptionEx);
                successMessage = "病历已保存，处方处理中，请到处方管理确认";
            }

            registrationService.completeVisit(registrationId);
            return ResponseEntity.ok(ApiResponse.success(successMessage));
        } catch (Exception ex) {
            log.error("保存病历失败 registrationId={}", registrationId, ex);
            return ResponseEntity.badRequest().body(ApiResponse.failure("保存病历失败：" + ex.getMessage()));
        }
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
