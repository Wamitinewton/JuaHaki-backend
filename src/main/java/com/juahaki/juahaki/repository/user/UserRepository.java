package com.juahaki.juahaki.repository.user;

import com.juahaki.juahaki.enums.AuthProvider;
import com.juahaki.juahaki.enums.Role;
import com.juahaki.juahaki.model.user.User;
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

    Optional<User> findByUsername(String username);

    Boolean existsByEmail(String email);

    Boolean existsByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username, String email);


    long countByIsEnabledTrue();

    long countByIsEnabledFalse();

    long countByIsAccountNonLockedFalse();

    long countByEmailVerifiedFalse();

    long countByRole(Role role);

    long countByProvider(AuthProvider provider);

    long countByProviderNot(AuthProvider provider);

    long countByCreatedAtAfter(LocalDateTime date);

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.firstName) LIKE %:searchTerm% OR " +
            "LOWER(u.lastName) LIKE %:searchTerm% OR " +
            "LOWER(u.username) LIKE %:searchTerm% OR " +
            "LOWER(u.email) LIKE %:searchTerm%")
    List<User> findBySearchTerm(@Param("searchTerm") String searchTerm);

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findRecentlyRegisteredUsers(@Param("limit") int limit);

    @Query("SELECT u FROM User u WHERE u.updatedAt < :cutoffDate AND u.isEnabled = true")
    List<User> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    List<User> findByRole(Role role);

    List<User> findByProvider(AuthProvider provider);

    List<User> findByEmailVerified(Boolean emailVerified);

    List<User> findByIsEnabled(Boolean isEnabled);

    List<User> findByIsAccountNonLocked(Boolean isAccountNonLocked);

    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<User> findByUpdatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT u FROM User u WHERE u.isEnabled = false AND u.createdAt > :date")
    List<User> findRecentlyDeactivatedUsers(@Param("date") LocalDateTime date);

    @Query("SELECT u FROM User u WHERE u.emailVerified = false AND u.createdAt < :date")
    List<User> findUnverifiedUsersOlderThan(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(u) FROM User u WHERE u.provider = :provider AND u.createdAt BETWEEN :startDate AND :endDate")
    long countByProviderAndCreatedAtBetween(@Param("provider") AuthProvider provider,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
}
