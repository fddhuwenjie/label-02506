package com.clinic.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class PatientExcel {
    @ExcelProperty("就诊卡号")
    @ColumnWidth(20)
    private String medicalCardNo;
    
    @ExcelProperty("姓名")
    @ColumnWidth(12)
    private String realName;
    
    @ExcelProperty("性别")
    @ColumnWidth(8)
    private String gender;
    
    @ExcelProperty("出生日期")
    @ColumnWidth(15)
    private String birthDate;
    
    @ExcelProperty("手机号")
    @ColumnWidth(15)
    private String phone;
    
    @ExcelProperty("血型")
    @ColumnWidth(8)
    private String bloodType;
    
    @ExcelProperty("家庭住址")
    @ColumnWidth(30)
    private String address;
    
    @ExcelProperty("过敏史")
    @ColumnWidth(20)
    private String allergyHistory;
    
    @ExcelProperty("建档时间")
    @ColumnWidth(20)
    private String createTime;
}
