package com.juahaki.juahaki.repository.user;

import com.juahaki.juahaki.enums.AuthProvider;
import com.juahaki.juahaki.enums.Role;
import com.juahaki.juahaki.model.user.User;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserQueryRepository {
    List<User> findUsersWithValidFcmTokens();
    List<User> findUsersWithValidFcmTokensByRole(Role role);
    List<User> findUsersWithValidFcmTokensByIds(List<Long> userIds);
    boolean existsByRole(Role role);
    long countByIsEnabledTrue();
    long countByIsEnabledFalse();
    long countByIsAccountNonLockedFalse();
    long countByEmailVerifiedFalse();
    long countByRole(Role role);
    long countByProvider(AuthProvider provider);
    long countByProviderNot(AuthProvider provider);
    long countByCreatedAtAfter(LocalDateTime date);
    List<User> findBySearchTerm(String searchTerm);
    List<User> findRecentlyRegisteredUsers(int limit);
    List<User> findInactiveUsers(LocalDateTime cutoffDate);
}