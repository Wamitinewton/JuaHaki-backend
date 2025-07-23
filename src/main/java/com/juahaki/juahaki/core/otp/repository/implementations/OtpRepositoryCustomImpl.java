package com.juahaki.juahaki.core.otp.repository.implementations;


import com.juahaki.juahaki.core.otp.repository.OtpRepositoryCustom;
import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.OtpType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class OtpRepositoryCustomImpl implements OtpRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void markExpiredOtps(LocalDateTime now) {
        entityManager.createQuery(
                        "UPDATE Otp o SET o.isExpired = true WHERE o.expiresAt <= :now")
                .setParameter("now", now)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void invalidateUserOtpsByType(User user, OtpType type) {
        entityManager.createQuery(
                        "UPDATE Otp o SET o.isExpired = true WHERE o.user = :user AND o.type = :type AND o.isUsed = false")
                .setParameter("user", user)
                .setParameter("type", type)
                .executeUpdate();
    }

    @Override
    @Transactional
    public void deleteOldOtps(LocalDateTime cutoffDate) {
        entityManager.createQuery(
                        "DELETE FROM Otp o WHERE o.createdAt <= :cutoffDate")
                .setParameter("cutoffDate", cutoffDate)
                .executeUpdate();
    }
}
