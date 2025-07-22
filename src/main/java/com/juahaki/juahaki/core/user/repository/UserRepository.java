package com.juahaki.juahaki.core.user.repository;

import com.juahaki.juahaki.shared.enums.AuthProvider;
import com.juahaki.juahaki.shared.enums.Role;
import com.juahaki.juahaki.core.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);
    Optional<User> findByUsernameOrEmail(String username, String email);
    Optional<User> findByFcmToken(String fcmToken);
}