package com.juahaki.juahaki.core.user.service.admin;


import com.juahaki.juahaki.core.user.dto.admin.AdminUserPageResponse;
import com.juahaki.juahaki.core.user.dto.admin.AdminUserResponse;
import com.juahaki.juahaki.core.user.dto.admin.UserFilterRequest;
import com.juahaki.juahaki.core.user.dto.admin.UserStatsResponse;
import com.juahaki.juahaki.shared.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IAdminUserManagementService {


    AdminUserPageResponse getAllUsers(HttpServletRequest request, UserFilterRequest filterRequest, Pageable pageable);


    AdminUserResponse getUserById(HttpServletRequest request, Long userId);


    void deleteUser(HttpServletRequest request, Long userId);


    AdminUserResponse lockUser(HttpServletRequest request, Long userId);


    AdminUserResponse unlockUser(HttpServletRequest request, Long userId);


    AdminUserResponse changeUserRole(HttpServletRequest request, Long userId, Role newRole);


    UserStatsResponse getUserStatistics(HttpServletRequest request);


    List<AdminUserResponse> searchUsers(HttpServletRequest request, String searchTerm);


    List<AdminUserResponse> getRecentlyRegisteredUsers(HttpServletRequest request, int limit);


    List<AdminUserResponse> getInactiveUsers(HttpServletRequest request, int daysSinceLastActivity);


    void bulkDeleteUsers(HttpServletRequest request, List<Long> userIds);

    void bulkActivateUsers(HttpServletRequest request, List<Long> userIds);

    void bulkDeactivateUsers(HttpServletRequest request, List<Long> userIds);
}