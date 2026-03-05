package com.clinic.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyStatDTO {
    private LocalDate date;
    private Long registrations;
    private Long visits;
    private BigDecimal revenue;
}
