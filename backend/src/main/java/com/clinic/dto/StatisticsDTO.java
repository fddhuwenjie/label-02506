package com.clinic.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StatisticsDTO {
    private Long totalRegistrations;      // 总挂号数
    private Long completedVisits;         // 完成就诊数
    private Long cancelledRegistrations;  // 取消挂号数
    private Long totalPrescriptions;      // 处方数
    private BigDecimal totalRevenue;      // 总营收
    private BigDecimal registrationFee;   // 挂号费收入
    private BigDecimal medicineFee;       // 药品费收入
    private Long newPatients;             // 新增病人数
}
