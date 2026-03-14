package com.clinic.controller;

import com.clinic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }
    
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                           @RequestParam(required = false) String logout,
                           Model model) {
        if (error != null) {
            model.addAttribute("error", "用户名或密码错误");
        }
        if (logout != null) {
            model.addAttribute("message", "已成功退出登录");
        }
        return "login";
    }
    
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }
    
    @PostMapping("/register")
    public String register(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String confirmPassword,
                          @RequestParam String realName,
                          @RequestParam String phone,
                          RedirectAttributes redirectAttributes) {
        if (username == null || username.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "用户名不能为空");
            return "redirect:/register";
        }
        if (username.length() < 3 || username.length() > 20) {
            redirectAttributes.addFlashAttribute("error", "用户名长度必须在3-20个字符之间");
            return "redirect:/register";
        }
        if (realName == null || realName.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "真实姓名不能为空");
            return "redirect:/register";
        }
        if (password == null || password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "密码长度至少6位");
            return "redirect:/register";
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "两次密码输入不一致");
            return "redirect:/register";
        }
        
        if (userService.existsByUsername(username)) {
            redirectAttributes.addFlashAttribute("error", "用户名已存在");
            return "redirect:/register";
        }
        
        try {
            userService.registerPatient(username, password, realName, phone);
            redirectAttributes.addFlashAttribute("message", "注册成功，请登录");
            return "redirect:/login";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "注册失败，请稍后重试");
            return "redirect:/register";
        }
    }
}
