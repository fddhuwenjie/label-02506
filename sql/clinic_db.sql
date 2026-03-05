-- ============================================
-- 私立医院门诊管理系统 - 数据库设计
-- 数据库名：clinic_db
-- 字符集：utf8mb4
-- 创建时间：2026-02-24
-- ============================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 创建数据库（Docker环境下数据库已由docker-compose创建）
-- DROP DATABASE IF EXISTS clinic_db;
-- CREATE DATABASE clinic_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE clinic_db;

-- ============================================
-- 1. 用户表（医生和病人统一管理）
-- ============================================
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名/登录账号',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    role TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0-病人，1-医生（管理员）',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(255) COMMENT '头像URL',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ============================================
-- 2. 病人档案表（扩展病人信息）
-- ============================================
CREATE TABLE patient_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '档案ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '关联用户ID',
    medical_card_no VARCHAR(50) UNIQUE COMMENT '就诊卡号（系统生成）',
    gender TINYINT COMMENT '性别：0-女，1-男',
    birth_date DATE COMMENT '出生日期',
    id_card VARCHAR(18) COMMENT '身份证号',
    address VARCHAR(255) COMMENT '家庭住址',
    emergency_contact VARCHAR(50) COMMENT '紧急联系人',
    emergency_phone VARCHAR(20) COMMENT '紧急联系电话',
    blood_type VARCHAR(10) COMMENT '血型',
    allergy_history TEXT COMMENT '过敏史',
    medical_history TEXT COMMENT '既往病史',
    remark TEXT COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建档时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    INDEX idx_medical_card (medical_card_no),
    INDEX idx_id_card (id_card)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病人档案表';

-- ============================================
-- 3. 医生信息表（扩展医生信息）
-- ============================================
CREATE TABLE doctor_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '医生档案ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '关联用户ID',
    employee_no VARCHAR(50) UNIQUE COMMENT '工号',
    title VARCHAR(50) COMMENT '职称（主治医师/副主任医师等）',
    specialty VARCHAR(100) COMMENT '专长',
    introduction TEXT COMMENT '简介',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生信息表';

-- ============================================
-- 4. 挂号规则配置表
-- ============================================
CREATE TABLE registration_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    week_day TINYINT NOT NULL COMMENT '星期几：1-7（周一到周日）',
    time_period TINYINT NOT NULL COMMENT '时段：1-上午，2-下午',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    max_count INT NOT NULL DEFAULT 20 COMMENT '最大挂号数',
    fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '挂号费',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停诊，1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (doctor_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    INDEX idx_doctor_weekday (doctor_id, week_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挂号规则配置表';

-- ============================================
-- 5. 挂号记录表
-- ============================================
CREATE TABLE registration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '挂号ID',
    reg_no VARCHAR(50) NOT NULL UNIQUE COMMENT '挂号单号',
    patient_id BIGINT NOT NULL COMMENT '病人ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    reg_date DATE NOT NULL COMMENT '挂号日期',
    time_period TINYINT NOT NULL COMMENT '时段：1-上午，2-下午',
    queue_no INT NOT NULL COMMENT '排队序号',
    fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '挂号费',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待就诊，1-就诊中，2-已完成，3-已取消，4-爽约',
    source TINYINT NOT NULL DEFAULT 0 COMMENT '来源：0-线上预约，1-现场挂号',
    cancel_reason VARCHAR(255) COMMENT '取消原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '挂号时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (patient_id) REFERENCES sys_user(id),
    FOREIGN KEY (doctor_id) REFERENCES sys_user(id),
    INDEX idx_reg_no (reg_no),
    INDEX idx_patient (patient_id),
    INDEX idx_doctor_date (doctor_id, reg_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挂号记录表';

-- ============================================
-- 6. 就诊病历表
-- ============================================
CREATE TABLE medical_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '病历ID',
    record_no VARCHAR(50) NOT NULL UNIQUE COMMENT '病历号',
    registration_id BIGINT NOT NULL COMMENT '关联挂号ID',
    patient_id BIGINT NOT NULL COMMENT '病人ID',
    doctor_id BIGINT NOT NULL COMMENT '接诊医生ID',
    visit_time DATETIME NOT NULL COMMENT '就诊时间',
    chief_complaint TEXT COMMENT '主诉',
    present_illness TEXT COMMENT '现病史',
    physical_exam TEXT COMMENT '体格检查',
    diagnosis VARCHAR(500) COMMENT '诊断结果',
    diagnosis_code VARCHAR(50) COMMENT '诊断编码（ICD-10）',
    treatment_plan TEXT COMMENT '治疗方案',
    doctor_advice TEXT COMMENT '医嘱',
    is_follow_up TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要复诊：0-否，1-是',
    follow_up_date DATE COMMENT '复诊日期',
    follow_up_note VARCHAR(255) COMMENT '复诊备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (registration_id) REFERENCES registration(id),
    FOREIGN KEY (patient_id) REFERENCES sys_user(id),
    FOREIGN KEY (doctor_id) REFERENCES sys_user(id),
    INDEX idx_record_no (record_no),
    INDEX idx_patient (patient_id),
    INDEX idx_doctor (doctor_id),
    INDEX idx_visit_time (visit_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='就诊病历表';

-- ============================================
-- 7. 药品信息表
-- ============================================
CREATE TABLE medicine (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '药品ID',
    medicine_code VARCHAR(50) NOT NULL UNIQUE COMMENT '药品编码',
    medicine_name VARCHAR(100) NOT NULL COMMENT '药品名称',
    generic_name VARCHAR(100) COMMENT '通用名',
    specification VARCHAR(100) COMMENT '规格',
    unit VARCHAR(20) NOT NULL COMMENT '单位（盒/瓶/支等）',
    manufacturer VARCHAR(200) COMMENT '生产厂家',
    category VARCHAR(50) COMMENT '药品分类',
    dosage_form VARCHAR(50) COMMENT '剂型（片剂/胶囊/注射液等）',
    purchase_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '进价',
    sell_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '售价',
    stock_quantity INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    warning_quantity INT NOT NULL DEFAULT 10 COMMENT '预警数量',
    shelf_life INT COMMENT '保质期（月）',
    storage_condition VARCHAR(100) COMMENT '存储条件',
    usage_method TEXT COMMENT '用法用量说明',
    contraindication TEXT COMMENT '禁忌',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_code (medicine_code),
    INDEX idx_name (medicine_name),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品信息表';

-- ============================================
-- 8. 药品库存变动记录表
-- ============================================
CREATE TABLE medicine_stock_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    medicine_id BIGINT NOT NULL COMMENT '药品ID',
    change_type TINYINT NOT NULL COMMENT '变动类型：1-入库，2-出库（处方），3-盘点调整，4-报损',
    change_quantity INT NOT NULL COMMENT '变动数量（正数入库，负数出库）',
    before_quantity INT NOT NULL COMMENT '变动前库存',
    after_quantity INT NOT NULL COMMENT '变动后库存',
    related_id BIGINT COMMENT '关联单据ID（处方ID/入库单ID等）',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    FOREIGN KEY (medicine_id) REFERENCES medicine(id),
    FOREIGN KEY (operator_id) REFERENCES sys_user(id),
    INDEX idx_medicine (medicine_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品库存变动记录表';

-- ============================================
-- 9. 处方表
-- ============================================
CREATE TABLE prescription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '处方ID',
    prescription_no VARCHAR(50) NOT NULL UNIQUE COMMENT '处方编号',
    medical_record_id BIGINT NOT NULL COMMENT '关联病历ID',
    patient_id BIGINT NOT NULL COMMENT '病人ID',
    doctor_id BIGINT NOT NULL COMMENT '开方医生ID',
    diagnosis VARCHAR(500) COMMENT '诊断',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '处方总金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待审核，1-已审核，2-已发药，3-已作废',
    audit_doctor_id BIGINT COMMENT '审核医生ID',
    audit_time DATETIME COMMENT '审核时间',
    dispense_time DATETIME COMMENT '发药时间',
    remark TEXT COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开方时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (medical_record_id) REFERENCES medical_record(id),
    FOREIGN KEY (patient_id) REFERENCES sys_user(id),
    FOREIGN KEY (doctor_id) REFERENCES sys_user(id),
    INDEX idx_prescription_no (prescription_no),
    INDEX idx_patient (patient_id),
    INDEX idx_doctor (doctor_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方表';

-- ============================================
-- 10. 处方明细表
-- ============================================
CREATE TABLE prescription_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    prescription_id BIGINT NOT NULL COMMENT '处方ID',
    medicine_id BIGINT NOT NULL COMMENT '药品ID',
    medicine_name VARCHAR(100) NOT NULL COMMENT '药品名称（冗余）',
    specification VARCHAR(100) COMMENT '规格（冗余）',
    unit VARCHAR(20) NOT NULL COMMENT '单位',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '单价',
    quantity INT NOT NULL COMMENT '数量',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
    dosage VARCHAR(50) COMMENT '单次剂量',
    frequency VARCHAR(50) COMMENT '用药频次（如：每日3次）',
    administration VARCHAR(50) COMMENT '给药途径（口服/外用/注射等）',
    days INT COMMENT '用药天数',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (prescription_id) REFERENCES prescription(id) ON DELETE CASCADE,
    FOREIGN KEY (medicine_id) REFERENCES medicine(id),
    INDEX idx_prescription (prescription_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方明细表';

-- ============================================
-- 11. 缴费记录表
-- ============================================
CREATE TABLE payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '缴费ID',
    payment_no VARCHAR(50) NOT NULL UNIQUE COMMENT '缴费单号',
    patient_id BIGINT NOT NULL COMMENT '病人ID',
    registration_id BIGINT COMMENT '关联挂号ID',
    prescription_id BIGINT COMMENT '关联处方ID',
    fee_type TINYINT NOT NULL COMMENT '费用类型：1-挂号费，2-诊疗费，3-药品费，4-其他',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '应付金额',
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
    payment_method TINYINT COMMENT '支付方式：1-现金，2-微信，3-支付宝，4-银行卡',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待支付，1-已支付，2-部分支付，3-已退款',
    operator_id BIGINT COMMENT '收费员ID',
    pay_time DATETIME COMMENT '支付时间',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (patient_id) REFERENCES sys_user(id),
    FOREIGN KEY (registration_id) REFERENCES registration(id),
    FOREIGN KEY (prescription_id) REFERENCES prescription(id),
    INDEX idx_payment_no (payment_no),
    INDEX idx_patient (patient_id),
    INDEX idx_status (status),
    INDEX idx_pay_time (pay_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缴费记录表';

-- ============================================
-- 12. 欠费记录表
-- ============================================
CREATE TABLE arrears (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '欠费ID',
    patient_id BIGINT NOT NULL COMMENT '病人ID',
    payment_id BIGINT NOT NULL COMMENT '关联缴费记录ID',
    arrears_amount DECIMAL(10,2) NOT NULL COMMENT '欠费金额',
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '已还金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-欠费中，1-已结清',
    clear_time DATETIME COMMENT '结清时间',
    remark VARCHAR(255) COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (patient_id) REFERENCES sys_user(id),
    FOREIGN KEY (payment_id) REFERENCES payment(id),
    INDEX idx_patient (patient_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='欠费记录表';

-- ============================================
-- 13. 系统操作日志表
-- ============================================
CREATE TABLE sys_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(50) COMMENT '用户名',
    operation VARCHAR(100) COMMENT '操作描述',
    method VARCHAR(200) COMMENT '请求方法',
    params TEXT COMMENT '请求参数',
    ip VARCHAR(50) COMMENT 'IP地址',
    duration BIGINT COMMENT '执行时长（毫秒）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-失败，1-成功',
    error_msg TEXT COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_user (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入默认管理员（医生）账号，密码：123456（BCrypt加密）
INSERT INTO sys_user (username, password, real_name, role, phone, status) VALUES
('admin', '$2a$10$nGFfzyG3QSU7epQ9uit4POwbZPQDUK/1z8aq8By6pXdE4Zls5Ie9i', '系统管理员', 1, '13800000000', 1),
('doctor1', '$2a$10$nGFfzyG3QSU7epQ9uit4POwbZPQDUK/1z8aq8By6pXdE4Zls5Ie9i', '张医生', 1, '13800000001', 1);

-- 插入医生档案
INSERT INTO doctor_profile (user_id, employee_no, title, specialty) VALUES
(1, 'D001', '主治医师', '全科'),
(2, 'D002', '副主任医师', '内科');

-- 插入测试病人账号，密码：123456（BCrypt加密）
INSERT INTO sys_user (username, password, real_name, role, phone, status) VALUES
('patient1', '$2a$10$nGFfzyG3QSU7epQ9uit4POwbZPQDUK/1z8aq8By6pXdE4Zls5Ie9i', '李小明', 0, '13900000001', 1),
('patient2', '$2a$10$nGFfzyG3QSU7epQ9uit4POwbZPQDUK/1z8aq8By6pXdE4Zls5Ie9i', '王小红', 0, '13900000002', 1);

-- 插入病人档案
INSERT INTO patient_profile (user_id, medical_card_no, gender, birth_date, id_card, address) VALUES
(3, 'MC202602240001', 1, '1990-05-15', '110101199005150011', '北京市朝阳区XX街道'),
(4, 'MC202602240002', 0, '1985-08-20', '110101198508200022', '北京市海淀区XX街道');

-- 插入示例药品数据
INSERT INTO medicine (medicine_code, medicine_name, generic_name, specification, unit, manufacturer, category, dosage_form, purchase_price, sell_price, stock_quantity, warning_quantity, usage_method) VALUES
('M001', '阿莫西林胶囊', '阿莫西林', '0.5g*24粒', '盒', '华北制药', '抗生素', '胶囊', 8.00, 15.00, 100, 20, '口服，一次0.5g，一日3次'),
('M002', '布洛芬缓释胶囊', '布洛芬', '0.3g*20粒', '盒', '中美史克', '解热镇痛', '胶囊', 12.00, 22.00, 80, 15, '口服，一次0.3-0.6g，一日2次'),
('M003', '复方感冒灵颗粒', '复方感冒灵', '10g*9袋', '盒', '三九医药', '感冒用药', '颗粒', 10.00, 18.00, 150, 30, '开水冲服，一次10g，一日3次'),
('M004', '奥美拉唑肠溶胶囊', '奥美拉唑', '20mg*14粒', '盒', '阿斯利康', '消化系统', '胶囊', 25.00, 45.00, 60, 10, '口服，一次20mg，一日1-2次'),
('M005', '氯雷他定片', '氯雷他定', '10mg*6片', '盒', '拜耳', '抗过敏', '片剂', 15.00, 28.00, 90, 15, '口服，一次10mg，一日1次');

-- 插入挂号规则（医生1的排班）
INSERT INTO registration_rule (doctor_id, week_day, time_period, start_time, end_time, max_count, fee) VALUES
(1, 1, 1, '08:00:00', '12:00:00', 20, 50.00),
(1, 1, 2, '14:00:00', '17:30:00', 15, 50.00),
(1, 3, 1, '08:00:00', '12:00:00', 20, 50.00),
(1, 3, 2, '14:00:00', '17:30:00', 15, 50.00),
(1, 5, 1, '08:00:00', '12:00:00', 20, 50.00),
(2, 2, 1, '08:00:00', '12:00:00', 25, 80.00),
(2, 2, 2, '14:00:00', '17:30:00', 20, 80.00),
(2, 4, 1, '08:00:00', '12:00:00', 25, 80.00),
(2, 4, 2, '14:00:00', '17:30:00', 20, 80.00);

-- ============================================
-- 插入更多测试病人
-- ============================================
INSERT INTO sys_user (username, password, real_name, role, phone, status) VALUES
('patient3', '$2a$10$nGFfzyG3QSU7epQ9uit4POwbZPQDUK/1z8aq8By6pXdE4Zls5Ie9i', '张三', 0, '13900000003', 1),
('patient4', '$2a$10$nGFfzyG3QSU7epQ9uit4POwbZPQDUK/1z8aq8By6pXdE4Zls5Ie9i', '李四', 0, '13900000004', 1),
('patient5', '$2a$10$nGFfzyG3QSU7epQ9uit4POwbZPQDUK/1z8aq8By6pXdE4Zls5Ie9i', '王五', 0, '13900000005', 1);

INSERT INTO patient_profile (user_id, medical_card_no, gender, birth_date, id_card, address, blood_type, allergy_history) VALUES
(5, 'MC202602240003', 1, '1988-03-10', '110101198803100033', '北京市西城区XX街道', 'A型', '青霉素过敏'),
(6, 'MC202602240004', 0, '1992-11-25', '110101199211250044', '北京市东城区XX街道', 'B型', NULL),
(7, 'MC202602240005', 1, '1978-07-08', '110101197807080055', '北京市丰台区XX街道', 'O型', '海鲜过敏');

-- ============================================
-- 插入挂号记录（历史数据 + 今日数据）
-- ============================================
INSERT INTO registration (reg_no, patient_id, doctor_id, reg_date, time_period, queue_no, fee, status, source, create_time) VALUES
-- 历史已完成挂号
('REG20260220001', 3, 1, '2026-02-20', 1, 1, 50.00, 2, 0, '2026-02-20 08:30:00'),
('REG20260220002', 4, 1, '2026-02-20', 1, 2, 50.00, 2, 1, '2026-02-20 08:45:00'),
('REG20260220003', 5, 2, '2026-02-20', 1, 1, 80.00, 2, 0, '2026-02-20 09:00:00'),
('REG20260221001', 3, 1, '2026-02-21', 1, 1, 50.00, 2, 0, '2026-02-21 08:20:00'),
('REG20260221002', 6, 1, '2026-02-21', 2, 1, 50.00, 2, 1, '2026-02-21 14:10:00'),
('REG20260222001', 4, 2, '2026-02-22', 1, 1, 80.00, 2, 0, '2026-02-22 08:30:00'),
('REG20260222002', 7, 2, '2026-02-22', 1, 2, 80.00, 2, 0, '2026-02-22 09:15:00'),
('REG20260223001', 5, 1, '2026-02-23', 1, 1, 50.00, 2, 1, '2026-02-23 08:40:00'),
('REG20260223002', 3, 1, '2026-02-23', 2, 1, 50.00, 3, 0, '2026-02-23 14:00:00'),
-- 今日挂号
('REG20260224001', 3, 1, CURDATE(), 1, 1, 50.00, 0, 0, NOW()),
('REG20260224002', 4, 1, CURDATE(), 1, 2, 50.00, 0, 1, NOW()),
('REG20260224003', 5, 1, CURDATE(), 1, 3, 50.00, 0, 0, NOW()),
('REG20260224004', 6, 1, CURDATE(), 2, 1, 50.00, 0, 0, NOW());

-- ============================================
-- 插入病历记录
-- ============================================
INSERT INTO medical_record (record_no, registration_id, patient_id, doctor_id, visit_time, chief_complaint, present_illness, physical_exam, diagnosis, treatment_plan, doctor_advice, is_follow_up, follow_up_date) VALUES
('MR20260220001', 1, 3, 1, '2026-02-20 09:00:00', '发热、咳嗽3天', '患者3天前受凉后出现发热，体温最高38.5℃，伴有咳嗽、咳少量白痰，无胸闷气促', '体温38.2℃，咽部充血，双肺呼吸音粗', '急性上呼吸道感染', '抗感染、对症治疗', '多饮水，注意休息，清淡饮食', 1, '2026-02-27'),
('MR20260220002', 2, 4, 1, '2026-02-20 09:30:00', '头痛、头晕2天', '患者2天前无明显诱因出现头痛，以前额部为主，伴头晕，无恶心呕吐', '血压130/85mmHg，神经系统检查未见异常', '紧张性头痛', '止痛、改善循环', '规律作息，避免熬夜，适当运动', 0, NULL),
('MR20260220003', 3, 5, 2, '2026-02-20 10:00:00', '胃痛、反酸1周', '患者1周前开始出现上腹部疼痛，进食后加重，伴反酸、烧心感', '上腹部轻压痛，无反跳痛', '慢性胃炎', '抑酸、保护胃黏膜', '规律饮食，忌辛辣刺激，戒烟酒', 1, '2026-03-06'),
('MR20260221001', 4, 3, 1, '2026-02-21 08:45:00', '复诊-感冒好转', '患者服药后体温恢复正常，咳嗽明显减轻', '体温36.5℃，咽部轻度充血', '急性上呼吸道感染（恢复期）', '继续对症治疗', '继续休息，巩固治疗', 0, NULL),
('MR20260221002', 5, 6, 1, '2026-02-21 14:30:00', '皮肤瘙痒、红疹3天', '患者3天前进食海鲜后出现全身皮肤瘙痒，伴红色皮疹', '躯干及四肢可见散在红色丘疹，部分融合成片', '过敏性皮炎', '抗过敏、止痒', '避免接触过敏原，忌食海鲜', 1, '2026-02-28'),
('MR20260222001', 6, 4, 2, '2026-02-22 09:00:00', '腹泻、腹痛2天', '患者2天前进食不洁食物后出现腹泻，水样便，每日5-6次，伴腹痛', '腹部轻度压痛，肠鸣音活跃', '急性肠胃炎', '补液、止泻、抗感染', '清淡饮食，多补充水分和电解质', 0, NULL),
('MR20260222002', 7, 7, 2, '2026-02-22 09:45:00', '咳嗽、气喘1周', '患者1周前感冒后出现咳嗽，近3天加重，伴气喘，夜间明显', '双肺可闻及哮鸣音', '支气管炎', '止咳、平喘、抗感染', '避免受凉，远离烟尘刺激', 1, '2026-03-01'),
('MR20260223001', 8, 5, 1, '2026-02-23 09:00:00', '复诊-胃痛缓解', '患者服药后胃痛明显缓解，反酸减轻', '上腹部无压痛', '慢性胃炎（好转）', '继续服药巩固', '坚持规律饮食', 0, NULL);

-- ============================================
-- 插入处方记录
-- ============================================
INSERT INTO prescription (prescription_no, medical_record_id, patient_id, doctor_id, diagnosis, total_amount, status, audit_doctor_id, audit_time, create_time) VALUES
('RX20260220001', 1, 3, 1, '急性上呼吸道感染', 55.00, 2, 1, '2026-02-20 09:10:00', '2026-02-20 09:05:00'),
('RX20260220002', 2, 4, 1, '紧张性头痛', 44.00, 2, 1, '2026-02-20 09:40:00', '2026-02-20 09:35:00'),
('RX20260220003', 3, 5, 2, '慢性胃炎', 90.00, 2, 2, '2026-02-20 10:15:00', '2026-02-20 10:10:00'),
('RX20260221001', 4, 3, 1, '急性上呼吸道感染（恢复期）', 18.00, 2, 1, '2026-02-21 08:55:00', '2026-02-21 08:50:00'),
('RX20260221002', 5, 6, 1, '过敏性皮炎', 56.00, 2, 1, '2026-02-21 14:45:00', '2026-02-21 14:40:00'),
('RX20260222001', 6, 4, 2, '急性肠胃炎', 33.00, 2, 2, '2026-02-22 09:15:00', '2026-02-22 09:10:00'),
('RX20260222002', 7, 7, 2, '支气管炎', 37.00, 2, 2, '2026-02-22 10:00:00', '2026-02-22 09:55:00'),
('RX20260223001', 8, 5, 1, '慢性胃炎（好转）', 45.00, 1, NULL, NULL, '2026-02-23 09:10:00');

-- ============================================
-- 插入处方明细
-- ============================================
INSERT INTO prescription_detail (prescription_id, medicine_id, medicine_name, specification, unit, unit_price, quantity, amount, dosage, frequency, administration, days) VALUES
-- 处方1：感冒
(1, 1, '阿莫西林胶囊', '0.5g*24粒', '盒', 15.00, 2, 30.00, '0.5g', '每日3次', '口服', 5),
(1, 3, '复方感冒灵颗粒', '10g*9袋', '盒', 18.00, 1, 18.00, '10g', '每日3次', '冲服', 3),
(1, 2, '布洛芬缓释胶囊', '0.3g*20粒', '盒', 22.00, 1, 7.00, '0.3g', '发热时服用', '口服', 0),
-- 处方2：头痛
(2, 2, '布洛芬缓释胶囊', '0.3g*20粒', '盒', 22.00, 2, 44.00, '0.3g', '每日2次', '口服', 5),
-- 处方3：胃炎
(3, 4, '奥美拉唑肠溶胶囊', '20mg*14粒', '盒', 45.00, 2, 90.00, '20mg', '每日1次', '口服', 14),
-- 处方4：感冒复诊
(4, 3, '复方感冒灵颗粒', '10g*9袋', '盒', 18.00, 1, 18.00, '10g', '每日3次', '冲服', 3),
-- 处方5：过敏
(5, 5, '氯雷他定片', '10mg*6片', '盒', 28.00, 2, 56.00, '10mg', '每日1次', '口服', 7),
-- 处方6：肠胃炎
(6, 1, '阿莫西林胶囊', '0.5g*24粒', '盒', 15.00, 1, 15.00, '0.5g', '每日3次', '口服', 3),
(6, 3, '复方感冒灵颗粒', '10g*9袋', '盒', 18.00, 1, 18.00, '10g', '每日3次', '冲服', 3),
-- 处方7：支气管炎
(7, 1, '阿莫西林胶囊', '0.5g*24粒', '盒', 15.00, 1, 15.00, '0.5g', '每日3次', '口服', 5),
(7, 2, '布洛芬缓释胶囊', '0.3g*20粒', '盒', 22.00, 1, 22.00, '0.3g', '每日2次', '口服', 3),
-- 处方8：胃炎复诊
(8, 4, '奥美拉唑肠溶胶囊', '20mg*14粒', '盒', 45.00, 1, 45.00, '20mg', '每日1次', '口服', 7);

-- ============================================
-- 插入缴费记录
-- ============================================
INSERT INTO payment (payment_no, patient_id, registration_id, prescription_id, fee_type, total_amount, paid_amount, payment_method, status, operator_id, pay_time, create_time) VALUES
-- 挂号费
('PAY20260220001', 3, 1, NULL, 1, 50.00, 50.00, 1, 1, 1, '2026-02-20 08:35:00', '2026-02-20 08:30:00'),
('PAY20260220002', 4, 2, NULL, 1, 50.00, 50.00, 2, 1, 1, '2026-02-20 08:50:00', '2026-02-20 08:45:00'),
('PAY20260220003', 5, 3, NULL, 1, 80.00, 80.00, 3, 1, 2, '2026-02-20 09:05:00', '2026-02-20 09:00:00'),
-- 药品费
('PAY20260220004', 3, NULL, 1, 3, 55.00, 55.00, 1, 1, 1, '2026-02-20 09:20:00', '2026-02-20 09:15:00'),
('PAY20260220005', 4, NULL, 2, 3, 44.00, 44.00, 2, 1, 1, '2026-02-20 09:50:00', '2026-02-20 09:45:00'),
('PAY20260220006', 5, NULL, 3, 3, 90.00, 90.00, 4, 1, 2, '2026-02-20 10:25:00', '2026-02-20 10:20:00'),
-- 更多缴费记录
('PAY20260221001', 3, 4, NULL, 1, 50.00, 50.00, 1, 1, 1, '2026-02-21 08:25:00', '2026-02-21 08:20:00'),
('PAY20260221002', 6, 5, NULL, 1, 50.00, 50.00, 2, 1, 1, '2026-02-21 14:15:00', '2026-02-21 14:10:00'),
('PAY20260221003', 3, NULL, 4, 3, 18.00, 18.00, 1, 1, 1, '2026-02-21 09:00:00', '2026-02-21 08:55:00'),
('PAY20260221004', 6, NULL, 5, 3, 56.00, 56.00, 3, 1, 1, '2026-02-21 14:50:00', '2026-02-21 14:45:00'),
('PAY20260222001', 4, 6, NULL, 1, 80.00, 80.00, 2, 1, 2, '2026-02-22 08:35:00', '2026-02-22 08:30:00'),
('PAY20260222002', 7, 7, NULL, 1, 80.00, 80.00, 1, 1, 2, '2026-02-22 09:20:00', '2026-02-22 09:15:00'),
('PAY20260222003', 4, NULL, 6, 3, 33.00, 33.00, 2, 1, 2, '2026-02-22 09:20:00', '2026-02-22 09:15:00'),
('PAY20260222004', 7, NULL, 7, 3, 37.00, 37.00, 1, 1, 2, '2026-02-22 10:05:00', '2026-02-22 10:00:00'),
('PAY20260223001', 5, 8, NULL, 1, 50.00, 50.00, 3, 1, 1, '2026-02-23 08:45:00', '2026-02-23 08:40:00'),
('PAY20260223002', 5, NULL, 8, 3, 45.00, 30.00, 1, 2, 1, '2026-02-23 09:15:00', '2026-02-23 09:10:00');

-- ============================================
-- 插入欠费记录
-- ============================================
INSERT INTO arrears (patient_id, payment_id, arrears_amount, paid_amount, status, remark, create_time) VALUES
(5, 16, 15.00, 0.00, 0, '药品费欠款', '2026-02-23 09:15:00');

-- ============================================
-- 更新药品库存（模拟出库）
-- ============================================
UPDATE medicine SET stock_quantity = stock_quantity - 8 WHERE id = 1;  -- 阿莫西林
UPDATE medicine SET stock_quantity = stock_quantity - 5 WHERE id = 2;  -- 布洛芬
UPDATE medicine SET stock_quantity = stock_quantity - 4 WHERE id = 3;  -- 感冒灵
UPDATE medicine SET stock_quantity = stock_quantity - 3 WHERE id = 4;  -- 奥美拉唑
UPDATE medicine SET stock_quantity = stock_quantity - 2 WHERE id = 5;  -- 氯雷他定

-- ============================================
-- 视图：今日挂号统计
-- ============================================
CREATE VIEW v_today_registration AS
SELECT 
    r.doctor_id,
    u.real_name AS doctor_name,
    COUNT(*) AS total_count,
    SUM(CASE WHEN r.status = 0 THEN 1 ELSE 0 END) AS waiting_count,
    SUM(CASE WHEN r.status = 1 THEN 1 ELSE 0 END) AS treating_count,
    SUM(CASE WHEN r.status = 2 THEN 1 ELSE 0 END) AS completed_count,
    SUM(CASE WHEN r.status = 3 THEN 1 ELSE 0 END) AS cancelled_count
FROM registration r
JOIN sys_user u ON r.doctor_id = u.id
WHERE r.reg_date = CURDATE()
GROUP BY r.doctor_id, u.real_name;

-- ============================================
-- 视图：药品库存预警
-- ============================================
CREATE VIEW v_medicine_warning AS
SELECT 
    id, medicine_code, medicine_name, specification, unit,
    stock_quantity, warning_quantity,
    (warning_quantity - stock_quantity) AS shortage_quantity
FROM medicine
WHERE stock_quantity <= warning_quantity AND status = 1;

-- ============================================
-- 视图：病人欠费汇总
-- ============================================
CREATE VIEW v_patient_arrears AS
SELECT 
    a.patient_id,
    u.real_name AS patient_name,
    p.medical_card_no,
    SUM(a.arrears_amount - a.paid_amount) AS total_arrears
FROM arrears a
JOIN sys_user u ON a.patient_id = u.id
JOIN patient_profile p ON u.id = p.user_id
WHERE a.status = 0
GROUP BY a.patient_id, u.real_name, p.medical_card_no;
