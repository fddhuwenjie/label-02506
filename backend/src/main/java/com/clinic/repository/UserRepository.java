package com.clinic.repository;

import com.clinic.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByPhone(String phone);
    List<User> findByRole(Integer role);
    List<User> findByRoleAndStatus(Integer role, Integer status);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 0 AND u.createTime BETWEEN ?1 AND ?2")
    Long countNewPatientsByDateRange(LocalDateTime start, LocalDateTime end);
}
