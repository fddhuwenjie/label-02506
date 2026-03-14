package com.clinic.controller;

import com.clinic.entity.RegistrationRule;
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
    public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("doctors", List.of(userDetails.getUser()));
        model.addAttribute("allRules", registrationService.findRulesByDoctor(userDetails.getId()));
        return "doctor/rule-list";
    }
    
    @GetMapping("/add")
    public String addPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("doctors", List.of(userDetails.getUser()));
        return "doctor/rule-form";
    }
    
    @GetMapping("/{id}/edit")
    public String editPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                           @PathVariable Long id, 
                           Model model,
                           RedirectAttributes redirectAttributes) {
        RegistrationRule rule = registrationService.findRuleById(id);
        if (rule == null) {
            redirectAttributes.addFlashAttribute("error", "规则不存在");
            return "redirect:/doctor/rules";
        }
        if (!rule.getDoctor().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权编辑该规则");
            return "redirect:/doctor/rules";
        }
        model.addAttribute("rule", rule);
        model.addAttribute("doctors", List.of(userDetails.getUser()));
        return "doctor/rule-form";
    }
    
    @PostMapping("/save")
    public String save(@AuthenticationPrincipal CustomUserDetails userDetails,
                      @RequestParam Integer weekDay,
                      @RequestParam Integer timePeriod,
                      @RequestParam String startTime,
                      @RequestParam String endTime,
                      @RequestParam Integer maxCount,
                      @RequestParam BigDecimal fee,
                      @RequestParam(required = false) Long id,
                      RedirectAttributes redirectAttributes) {
        
        RegistrationRule rule;
        if (id != null) {
            rule = registrationService.findRuleById(id);
            if (rule == null) {
                redirectAttributes.addFlashAttribute("error", "规则不存在");
                return "redirect:/doctor/rules";
            }
            if (!rule.getDoctor().getId().equals(userDetails.getId())) {
                redirectAttributes.addFlashAttribute("error", "无权编辑该规则");
                return "redirect:/doctor/rules";
            }
        } else {
            rule = new RegistrationRule();
        }
        rule.setDoctor(userService.findById(userDetails.getId()));
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
    public String toggleStatus(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @PathVariable Long id) {
        RegistrationRule rule = registrationService.findRuleById(id);
        if (rule == null || !rule.getDoctor().getId().equals(userDetails.getId())) {
            return "error";
        }
        registrationService.toggleRuleStatus(id);
        return "success";
    }
    
    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal CustomUserDetails userDetails,
                         @PathVariable Long id, 
                         RedirectAttributes redirectAttributes) {
        RegistrationRule rule = registrationService.findRuleById(id);
        if (rule == null) {
            redirectAttributes.addFlashAttribute("error", "规则不存在");
            return "redirect:/doctor/rules";
        }
        if (!rule.getDoctor().getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("error", "无权删除该规则");
            return "redirect:/doctor/rules";
        }
        registrationService.deleteRule(id);
        redirectAttributes.addFlashAttribute("message", "挂号规则已删除");
        return "redirect:/doctor/rules";
    }
}
