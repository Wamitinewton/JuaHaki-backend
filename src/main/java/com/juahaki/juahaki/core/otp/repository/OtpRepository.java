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
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findByOtpCodeAndEmailAndTypeAndIsUsedFalseAndIsExpiredFalse(
            String otpCode, String email, OtpType type);

    Optional<Otp> findTopByEmailAndTypeAndIsUsedFalseAndIsExpiredFalseOrderByCreatedAtDesc(
            String email, OtpType type);

    @Query("SELECT o FROM Otp o WHERE o.user = :user AND o.type = :type AND o.isUsed = false AND o.isExpired = false ORDER BY o.createdAt DESC")
    Optional<Otp> findLatestValidOtpByUserAndType(@Param("user") User user, @Param("type") OtpType type);

    @Modifying
    @Query("UPDATE Otp o SET o.isExpired = true WHERE o.expiresAt <= :now")
    void markExpiredOtps(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Otp o SET o.isExpired = true WHERE o.user = :user AND o.type = :type AND o.isUsed = false")
    void invalidateUserOtpsByType(@Param("user") User user, @Param("type") OtpType type);

    @Modifying
    @Query("DELETE FROM Otp o WHERE o.createdAt <= :cutoffDate")
    void deleteOldOtps(@Param("cutoffDate") LocalDateTime cutoffDate);
}
