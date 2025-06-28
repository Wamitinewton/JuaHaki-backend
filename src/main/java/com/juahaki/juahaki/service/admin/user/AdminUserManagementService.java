package com.juahaki.juahaki.service.admin.user;

import com.juahaki.juahaki.dto.admin.user.AdminUserPageResponse;
import com.juahaki.juahaki.dto.admin.user.AdminUserResponse;
import com.juahaki.juahaki.dto.admin.user.UserFilterRequest;
import com.juahaki.juahaki.dto.admin.user.UserStatsResponse;
import com.juahaki.juahaki.enums.Role;
import com.juahaki.juahaki.exception.CustomException;
import com.juahaki.juahaki.mapper.AdminUserMapper;
import com.juahaki.juahaki.model.user.User;
import com.juahaki.juahaki.repository.user.UserRepository;
import com.juahaki.juahaki.service.email.IEmailService;
import com.juahaki.juahaki.util.jwt.JwtHelperService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserManagementService implements IAdminUserManagementService {

    private final UserRepository userRepository;
    private final JwtHelperService jwtHelperService;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;
    private final AdminUserMapper adminUserMapper;

    @Override
    public AdminUserPageResponse getAllUsers(HttpServletRequest request, UserFilterRequest filterRequest, Pageable pageable) {
        validateAdminAccess(request);

        Specification<User> spec = createUserSpecification(filterRequest);
        Page<User> users = userRepository.findAll(spec, pageable);

        return adminUserMapper.buildAdminUserPageResponse(users);
    }

    @Override
    public AdminUserResponse getUserById(HttpServletRequest request, Long userId) {
        validateAdminAccess(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found with ID: " + userId));

        return adminUserMapper.mapToAdminUserResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(HttpServletRequest request, Long userId) {
        validateAdminAccess(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found with ID: " + userId));

        // Prevent admin from deleting themselves
        Long currentUserId = jwtHelperService.getCurrentUserIdFromRequest(request);
        if (currentUserId.equals(userId)) {
            throw new CustomException("Cannot delete your own account");
        }

        userRepository.deleteById(userId);
        log.info("Admin {} deleted user with ID: {}", currentUserId, userId);
    }

    @Override
    @Transactional
    public AdminUserResponse lockUser(HttpServletRequest request, Long userId) {
        validateAdminAccess(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found with ID: " + userId));

        Long currentUserId = jwtHelperService.getCurrentUserIdFromRequest(request);
        if (currentUserId.equals(userId)) {
            throw new CustomException("Cannot lock your own account");
        }

        user.setAccountNonLocked(false);
        User savedUser = userRepository.save(user);

        try {
            emailService.sendAccountLockedNotification(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            log.warn("Failed to send account locked notification to user: {}", user.getEmail(), e);
        }

        log.info("Admin {} locked user with ID: {}", currentUserId, userId);

        return adminUserMapper.mapToAdminUserResponse(savedUser);
    }

    @Override
    @Transactional
    public AdminUserResponse unlockUser(HttpServletRequest request, Long userId) {
        validateAdminAccess(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found with ID: " + userId));

        if (user.isAccountNonLocked()) {
            throw new CustomException("Cannot unlock an already active account with ID: " + userId);
        }

        user.setAccountNonLocked(true);
        User savedUser = userRepository.save(user);

        try {
            emailService.sendAccountUnlockedNotification(user.getEmail(), user.getFirstName());
        } catch (Exception e) {
            log.warn("Failed to send account unlocked notification to user: {}", user.getEmail(), e);
        }

        log.info("Admin {} unlocked user with ID: {}",
                jwtHelperService.getCurrentUserIdFromRequest(request), userId);

        return adminUserMapper.mapToAdminUserResponse(savedUser);
    }

    @Override
    @Transactional
    public AdminUserResponse changeUserRole(HttpServletRequest request, Long userId, Role newRole) {
        validateAdminAccess(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found with ID: " + userId));

        Long currentUserId = jwtHelperService.getCurrentUserIdFromRequest(request);
        if (currentUserId.equals(userId)) {
            throw new CustomException("Cannot change your own role");
        }

        if (user.getRole() == newRole) {
            throw new CustomException("User already has the role: " + newRole.name());

        }

        Role oldRole = user.getRole();
        user.setRole(newRole);
        User savedUser = userRepository.save(user);

        try {
            emailService.sendRoleChangeNotification(user.getEmail(), user.getFirstName(), oldRole, newRole);
        } catch (Exception e) {
            log.warn("Failed to send role change notification to user: {}", user.getEmail(), e);
        }

        log.info("Admin {} changed role of user {} from {} to {}",
                currentUserId, userId, oldRole, newRole);

        return adminUserMapper.mapToAdminUserResponse(savedUser);
    }

    @Override
    public UserStatsResponse getUserStatistics(HttpServletRequest request) {
        validateAdminAccess(request);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1).toLocalDate().atStartOfDay();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();

        return UserStatsResponse.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.countByIsEnabledTrue())
                .inactiveUsers(userRepository.countByIsEnabledFalse())
                .lockedUsers(userRepository.countByIsAccountNonLockedFalse())
                .unverifiedUsers(userRepository.countByEmailVerifiedFalse())
                .adminUsers(userRepository.countByRole(Role.ADMIN))
                .regularUsers(userRepository.countByRole(Role.USER))
                .oauthUsers(userRepository.countByProviderNot(com.juahaki.juahaki.enums.AuthProvider.LOCAL))
                .localUsers(userRepository.countByProvider(com.juahaki.juahaki.enums.AuthProvider.LOCAL))
                .usersCreatedToday(userRepository.countByCreatedAtAfter(startOfDay))
                .usersCreatedThisWeek(userRepository.countByCreatedAtAfter(startOfWeek))
                .usersCreatedThisMonth(userRepository.countByCreatedAtAfter(startOfMonth))
                .build();
    }

    @Override
    public List<AdminUserResponse> searchUsers(HttpServletRequest request, String searchTerm) {
        validateAdminAccess(request);

        if (!StringUtils.hasText(searchTerm)) {
            return new ArrayList<>();
        }

        List<User> users = userRepository.findBySearchTerm(searchTerm.toLowerCase());
        return adminUserMapper.mapToAdminUserResponseList(users);
    }

    @Override
    public List<AdminUserResponse> getRecentlyRegisteredUsers(HttpServletRequest request, int limit) {
        validateAdminAccess(request);

        List<User> users = userRepository.findRecentlyRegisteredUsers(limit);
        return adminUserMapper.mapToAdminUserResponseList(users);
    }

    @Override
    public List<AdminUserResponse> getInactiveUsers(HttpServletRequest request, int daysSinceLastActivity) {
        validateAdminAccess(request);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysSinceLastActivity);
        List<User> users = userRepository.findInactiveUsers(cutoffDate);
        return adminUserMapper.mapToAdminUserResponseList(users);
    }

    @Override
    @Transactional
    public void bulkDeleteUsers(HttpServletRequest request, List<Long> userIds) {
        validateAdminAccess(request);

        Long currentUserId = jwtHelperService.getCurrentUserIdFromRequest(request);

        // Remove current user from the list to prevent self-deletion
        userIds.removeIf(id -> id.equals(currentUserId));

        if (userIds.isEmpty()) {
            throw new CustomException("No valid users to delete");
        }

        userRepository.deleteAllById(userIds);
        log.info("Admin {} bulk deleted {} users", currentUserId, userIds.size());
    }

    @Override
    @Transactional
    public void bulkActivateUsers(HttpServletRequest request, List<Long> userIds) {
        validateAdminAccess(request);

        if (userIds.isEmpty()) {
            throw new CustomException("No users specified for activation");
        }

        List<User> users = userRepository.findAllById(userIds);
        users.forEach(user -> user.setEnabled(true));
        userRepository.saveAll(users);

        log.info("Admin {} bulk activated {} users",
                jwtHelperService.getCurrentUserIdFromRequest(request), users.size());
    }

    @Override
    @Transactional
    public void bulkDeactivateUsers(HttpServletRequest request, List<Long> userIds) {
        validateAdminAccess(request);

        Long currentUserId = jwtHelperService.getCurrentUserIdFromRequest(request);

        // Remove current user from the list to prevent self-deactivation
        userIds.removeIf(id -> id.equals(currentUserId));

        if (userIds.isEmpty()) {
            throw new CustomException("No valid users to deactivate");
        }

        List<User> users = userRepository.findAllById(userIds);
        users.forEach(user -> user.setEnabled(false));
        userRepository.saveAll(users);

        log.info("Admin {} bulk deactivated {} users", currentUserId, users.size());
    }

    private void validateAdminAccess(HttpServletRequest request) {
        if (!jwtHelperService.isAdmin(request)) {
            throw new CustomException("Access denied. Admin privileges required.");
        }
    }

    private Specification<User> createUserSpecification(UserFilterRequest filterRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filterRequest != null) {
                if (filterRequest.getRole() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("role"), filterRequest.getRole()));
                }

                if (filterRequest.getProvider() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("provider"), filterRequest.getProvider()));
                }

                if (filterRequest.getEmailVerified() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("emailVerified"), filterRequest.getEmailVerified()));
                }

                if (filterRequest.getIsEnabled() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("isEnabled"), filterRequest.getIsEnabled()));
                }

                if (filterRequest.getIsAccountNonLocked() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("isAccountNonLocked"), filterRequest.getIsAccountNonLocked()));
                }

                if (StringUtils.hasText(filterRequest.getSearchTerm())) {
                    String searchPattern = "%" + filterRequest.getSearchTerm().toLowerCase() + "%";
                    Predicate searchPredicate = criteriaBuilder.or(
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchPattern),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchPattern),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), searchPattern),
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchPattern)
                    );
                    predicates.add(searchPredicate);
                }

                if (filterRequest.getCreatedAfter() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filterRequest.getCreatedAfter()));
                }

                if (filterRequest.getCreatedBefore() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filterRequest.getCreatedBefore()));
                }

                if (filterRequest.getUpdatedAfter() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), filterRequest.getUpdatedAfter()));
                }

                if (filterRequest.getUpdatedBefore() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("updatedAt"), filterRequest.getUpdatedBefore()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}