package com.clinic.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class StatisticsExcel {
    @ExcelProperty("日期")
    @ColumnWidth(15)
    private String date;
    
    @ExcelProperty("挂号数")
    @ColumnWidth(12)
    private Long registrations;
    
    @ExcelProperty("就诊数")
    @ColumnWidth(12)
    private Long visits;
    
    @ExcelProperty("营收(元)")
    @ColumnWidth(15)
    private String revenue;
}
