package com.juahaki.juahaki.core.otp.repository;

import com.juahaki.juahaki.shared.enums.OtpType;
import com.juahaki.juahaki.core.otp.model.Otp;
import com.juahaki.juahaki.core.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long>, OtpRepositoryCustom {

    Optional<Otp> findByOtpCodeAndEmailAndTypeAndIsUsedFalseAndIsExpiredFalse(
            String otpCode, String email, OtpType type);

    Optional<Otp> findTopByEmailAndTypeAndIsUsedFalseAndIsExpiredFalseOrderByCreatedAtDesc(
            String email, OtpType type);
}
