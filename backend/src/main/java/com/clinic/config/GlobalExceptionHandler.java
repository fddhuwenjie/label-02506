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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("业务异常 [{}][{}]: {}", request.getRequestURI(), ex.getCode(), ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("参数错误 [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.warn("业务状态异常 [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex, HttpServletRequest request) {
        log.warn("资源不存在 [{}]: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("请求的资源不存在"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("缺少必要参数 [{}]: {}", request.getRequestURI(), ex.getParameterName());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("缺少必要参数：" + ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("参数类型错误 [{}]: {} 应为 {}", request.getRequestURI(), ex.getName(), ex.getRequiredType());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("参数格式错误：" + ex.getName()));
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.error("系统异常 [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        String accept = request.getHeader("Accept");
        String xRequestedWith = request.getHeader("X-Requested-With");
        boolean isAjax = "XMLHttpRequest".equals(xRequestedWith) 
                || (accept != null && accept.contains("application/json"));

        if (isAjax) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("系统繁忙，请稍后重试"));
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "操作失败，请稍后重试");
            return new ModelAndView("redirect:" + extractPath(referer));
        }

        redirectAttributes.addFlashAttribute("error", "系统异常，请稍后重试");
        return new ModelAndView("redirect:/");
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
}
