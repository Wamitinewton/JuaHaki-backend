package com.juahaki.juahaki.core.user.repository;


import com.juahaki.juahaki.core.user.model.User;
import com.juahaki.juahaki.shared.enums.AuthProvider;
import com.juahaki.juahaki.shared.enums.Role;
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