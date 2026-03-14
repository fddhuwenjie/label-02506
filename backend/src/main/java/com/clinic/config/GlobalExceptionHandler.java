package com.clinic.config;

import com.clinic.dto.ApiResponse;
import com.clinic.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private boolean isAjaxRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String xRequestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(xRequestedWith) 
                || (accept != null && accept.contains("application/json"));
    }

    private String getRedirectPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return extractPath(referer);
        }
        return "/";
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.warn("业务异常 [{}][{}]: {}", request.getRequestURI(), ex.getCode(), ex.getMessage());
        
        if (isAjaxRequest(request)) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(ex.getMessage()));
        }
        
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return new ModelAndView("redirect:" + getRedirectPath(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.warn("参数错误 [{}]: {}", request.getRequestURI(), ex.getMessage());
        
        if (isAjaxRequest(request)) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(ex.getMessage()));
        }
        
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return new ModelAndView("redirect:" + getRedirectPath(request));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalState(IllegalStateException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.warn("业务状态异常 [{}]: {}", request.getRequestURI(), ex.getMessage());
        
        if (isAjaxRequest(request)) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(ex.getMessage()));
        }
        
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return new ModelAndView("redirect:" + getRedirectPath(request));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public Object handleNotFound(NoSuchElementException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.warn("资源不存在 [{}]: {}", request.getRequestURI(), ex.getMessage());
        
        if (isAjaxRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure("请求的资源不存在"));
        }
        
        redirectAttributes.addFlashAttribute("error", "请求的资源不存在");
        return new ModelAndView("redirect:" + getRedirectPath(request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Object handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.warn("缺少必要参数 [{}]: {}", request.getRequestURI(), ex.getParameterName());
        String message = "请填写必填项：" + getParamDisplayName(ex.getParameterName());
        
        if (isAjaxRequest(request)) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(message));
        }
        
        redirectAttributes.addFlashAttribute("error", message);
        return new ModelAndView("redirect:" + getRedirectPath(request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.warn("参数类型错误 [{}]: {} 应为 {}", request.getRequestURI(), ex.getName(), ex.getRequiredType());
        String message = "参数格式错误：" + getParamDisplayName(ex.getName());
        
        if (isAjaxRequest(request)) {
            return ResponseEntity.badRequest().body(ApiResponse.failure(message));
        }
        
        redirectAttributes.addFlashAttribute("error", message);
        return new ModelAndView("redirect:" + getRedirectPath(request));
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.error("系统异常 [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        if (isAjaxRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("系统繁忙，请稍后重试"));
        }

        redirectAttributes.addFlashAttribute("error", "操作失败，请稍后重试");
        return new ModelAndView("redirect:" + getRedirectPath(request));
    }

    private String extractPath(String url) {
        try {
            java.net.URL parsed = new java.net.URL(url);
            String path = parsed.getPath();
            return path != null && !path.isEmpty() ? path : "/";
        } catch (Exception e) {
            return "/";
        }
    }

    private String getParamDisplayName(String paramName) {
        return switch (paramName) {
            case "doctorId" -> "医生";
            case "patientId" -> "病人";
            case "date" -> "日期";
            case "timePeriod" -> "时段";
            case "username" -> "用户名";
            case "password" -> "密码";
            case "realName" -> "真实姓名";
            case "phone" -> "手机号";
            case "medicineId" -> "药品";
            case "quantity" -> "数量";
            case "amount" -> "金额";
            case "paidAmount" -> "缴费金额";
            case "paymentMethod" -> "支付方式";
            default -> paramName;
        };
    }
}
