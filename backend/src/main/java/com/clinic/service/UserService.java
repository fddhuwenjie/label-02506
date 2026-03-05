package com.clinic.service;

import com.clinic.entity.PatientProfile;
import com.clinic.entity.User;
import com.clinic.repository.PatientProfileRepository;
import com.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final PasswordEncoder passwordEncoder;
    
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
    
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public List<User> findDoctors() {
        return userRepository.findByRoleAndStatus(1, 1);
    }
    
    public List<User> findPatients() {
        return userRepository.findByRole(0);
    }
    
    @Transactional
    public User registerPatient(String username, String password, String realName, String phone) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setRole(0);
        user.setStatus(1);
        user = userRepository.save(user);
        
        // 创建病人档案
        PatientProfile profile = new PatientProfile();
        profile.setUser(user);
        profile.setMedicalCardNo(generateMedicalCardNo());
        patientProfileRepository.save(profile);
        
        return user;
    }
    
    private String generateMedicalCardNo() {
        String prefix = "MC" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Integer maxNo = patientProfileRepository.findMaxCardNoByPrefix(prefix);
        return prefix + String.format("%04d", maxNo + 1);
    }
    
    @Transactional
    public void updateUser(User user) {
        userRepository.save(user);
    }
    
    public PatientProfile getPatientProfile(Long userId) {
        return patientProfileRepository.findByUserId(userId).orElse(null);
    }
    
    @Transactional
    public void updatePatientProfile(PatientProfile profile) {
        patientProfileRepository.save(profile);
    }
}
