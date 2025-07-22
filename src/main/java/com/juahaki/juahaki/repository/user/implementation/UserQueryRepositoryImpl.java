package com.juahaki.juahaki.repository.user.implementation;

import com.juahaki.juahaki.enums.AuthProvider;
import com.juahaki.juahaki.enums.Role;
import com.juahaki.juahaki.model.user.User;
import com.juahaki.juahaki.repository.user.UserQueryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Repository
public class UserQueryRepositoryImpl implements UserQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<User> findUsersWithValidFcmTokens() {
        String jpql = "SELECT u FROM User u WHERE u.fcmToken IS NOT NULL AND u.pushNotificationsEnabled = true";
        return entityManager.createQuery(jpql, User.class).getResultList();
    }

    @Override
    public List<User> findUsersWithValidFcmTokensByRole(Role role) {
        String jpql = "SELECT u FROM User u WHERE u.fcmToken IS NOT NULL AND u.pushNotificationsEnabled = true AND u.role = :role";
        return entityManager.createQuery(jpql, User.class)
                .setParameter("role", role)
                .getResultList();
    }

    @Override
    public List<User> findUsersWithValidFcmTokensByIds(List<Long> userIds) {
        String jpql = "SELECT u FROM User u WHERE u.fcmToken IS NOT NULL AND u.pushNotificationsEnabled = true AND u.id IN :userIds";
        return entityManager.createQuery(jpql, User.class)
                .setParameter("userIds", userIds)
                .getResultList();
    }

    @Override
    public boolean existsByRole(Role role) {
        String jpql = "SELECT COUNT(u) FROM User u WHERE u.role = :role";
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("role", role)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public long countByIsEnabledTrue() {
        String jpql = "SELECT COUNT(u) FROM User u WHERE u.isEnabled = true";
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }

    @Override
    public long countByIsEnabledFalse() {
        String jpql = "SELECT COUNT(u) FROM User u WHERE u.isEnabled = false";
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }

    @Override
    public long countByIsAccountNonLockedFalse() {
        String jpql = "SELECT COUNT(u) FROM User u WHERE u.isAccountNonLocked = false";
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }

    @Override
    public long countByEmailVerifiedFalse() {
        String jpql = "SELECT COUNT(u) FROM User u WHERE u.emailVerified = false";
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }

    @Override
    public long countByRole(Role role) {
        String jpql = "SELECT COUNT(u) FROM User u WHERE u.role = :role";
        return entityManager.createQuery(jpql, Long.class)
                .setParameter("role", role)
                .getSingleResult();
    }

    @Override
    public long countByProvider(AuthProvider provider) {
        String jpql = "SELECT COUNT(u) FROM User u WHERE u.provider = :provider";
        return entityManager.createQuery(jpql, Long.class)
                .setParameter("provider", provider)
                .getSingleResult();
    }

    @Override
    public long countByProviderNot(AuthProvider provider) {
        String jpql = "SELECT COUNT(u) FROM User u WHERE u.provider != :provider";
        return entityManager.createQuery(jpql, Long.class)
                .setParameter("provider", provider)
                .getSingleResult();
    }

    @Override
    public long countByCreatedAtAfter(LocalDateTime date) {
        String jpql = "SELECT COUNT(u) FROM User u WHERE u.createdAt > :date";
        return entityManager.createQuery(jpql, Long.class)
                .setParameter("date", date)
                .getSingleResult();
    }

    @Override
    public List<User> findBySearchTerm(String searchTerm) {
        String jpql = "SELECT u FROM User u WHERE " +
                "LOWER(u.firstName) LIKE :searchTerm OR " +
                "LOWER(u.lastName) LIKE :searchTerm OR " +
                "LOWER(u.username) LIKE :searchTerm OR " +
                "LOWER(u.email) LIKE :searchTerm";

        String searchPattern = "%" + searchTerm.toLowerCase() + "%";
        return entityManager.createQuery(jpql, User.class)
                .setParameter("searchTerm", searchPattern)
                .getResultList();
    }

    @Override
    public List<User> findRecentlyRegisteredUsers(int limit) {
        String jpql = "SELECT u FROM User u ORDER BY u.createdAt DESC";
        TypedQuery<User> query = entityManager.createQuery(jpql, User.class);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public List<User> findInactiveUsers(LocalDateTime cutoffDate) {
        String jpql = "SELECT u FROM User u WHERE u.updatedAt < :cutoffDate AND u.isEnabled = true";
        return entityManager.createQuery(jpql, User.class)
                .setParameter("cutoffDate", cutoffDate)
                .getResultList();
    }
}

