package com.clinic.controller;

import com.clinic.entity.RegistrationRule;
import com.clinic.entity.User;
import com.clinic.security.CustomUserDetails;
import com.clinic.service.RegistrationService;
import com.clinic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/doctor/rules")
@RequiredArgsConstructor
public class RegistrationRuleController {
    private final RegistrationService registrationService;
    private final UserService userService;
    
    @GetMapping
    public String list(Model model) {
        List<User> doctors = userService.findDoctors();
        model.addAttribute("doctors", doctors);
        model.addAttribute("allRules", registrationService.findAllRules());
        return "doctor/rule-list";
    }
    
    @GetMapping("/add")
    public String addPage(Model model) {
        model.addAttribute("doctors", userService.findDoctors());
        return "doctor/rule-form";
    }
    
    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        model.addAttribute("rule", registrationService.findRuleById(id));
        model.addAttribute("doctors", userService.findDoctors());
        return "doctor/rule-form";
    }
    
    @PostMapping("/save")
    public String save(@RequestParam Long doctorId,
                      @RequestParam Integer weekDay,
                      @RequestParam Integer timePeriod,
                      @RequestParam String startTime,
                      @RequestParam String endTime,
                      @RequestParam Integer maxCount,
                      @RequestParam BigDecimal fee,
                      @RequestParam(required = false) Long id,
                      RedirectAttributes redirectAttributes) {
        
        RegistrationRule rule = id != null ? registrationService.findRuleById(id) : new RegistrationRule();
        rule.setDoctor(userService.findById(doctorId));
        rule.setWeekDay(weekDay);
        rule.setTimePeriod(timePeriod);
        rule.setStartTime(LocalTime.parse(startTime));
        rule.setEndTime(LocalTime.parse(endTime));
        rule.setMaxCount(maxCount);
        rule.setFee(fee);
        rule.setStatus(1);
        
        registrationService.saveRule(rule);
        redirectAttributes.addFlashAttribute("message", "挂号规则保存成功");
        return "redirect:/doctor/rules";
    }
    
    @PostMapping("/{id}/toggle")
    @ResponseBody
    public String toggleStatus(@PathVariable Long id) {
        registrationService.toggleRuleStatus(id);
        return "success";
    }
    
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        registrationService.deleteRule(id);
        redirectAttributes.addFlashAttribute("message", "挂号规则已删除");
        return "redirect:/doctor/rules";
    }
}
