package com.juahaki.juahaki.controller.admin.user;

import com.juahaki.juahaki.dto.admin.user.AdminUserResponse;
import com.juahaki.juahaki.dto.admin.user.UserFilterRequest;
import com.juahaki.juahaki.dto.admin.user.UserStatsResponse;
import com.juahaki.juahaki.enums.Role;
import com.juahaki.juahaki.response.ApiResponse;
import com.juahaki.juahaki.service.admin.user.IAdminUserManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.prefix}/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final IAdminUserManagementService adminUserManagementService;

    @GetMapping("/get-all-users")
    public ResponseEntity<ApiResponse> getAllUsers(
            HttpServletRequest request,
            @Valid UserFilterRequest filterRequest,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<AdminUserResponse> users = adminUserManagementService.getAllUsers(request, filterRequest, pageable);
        return ResponseEntity.ok(new ApiResponse("Users retrieved successfully", users));
    }

    @GetMapping("/get-by-id/{userId}")
    public ResponseEntity<ApiResponse> getUserById(
            HttpServletRequest request,
            @PathVariable Long userId) {

        AdminUserResponse user = adminUserManagementService.getUserById(request, userId);
        return ResponseEntity.ok(new ApiResponse("User retrieved successfully", user));
    }

    @DeleteMapping("/delete-by-id/{userId}")
    public ResponseEntity<ApiResponse> deleteUser(
            HttpServletRequest request,
            @PathVariable Long userId) {

        adminUserManagementService.deleteUser(request, userId);
        return ResponseEntity.ok(new ApiResponse("User deleted successfully", null));
    }

    @PutMapping("/{userId}/lock")
    public ResponseEntity<ApiResponse> lockUser(
            HttpServletRequest request,
            @PathVariable Long userId) {

        AdminUserResponse user = adminUserManagementService.lockUser(request, userId);
        return ResponseEntity.ok(new ApiResponse("User locked successfully", user));
    }

    @PutMapping("/{userId}/unlock")
    public ResponseEntity<ApiResponse> unlockUser(
            HttpServletRequest request,
            @PathVariable Long userId) {

        AdminUserResponse user = adminUserManagementService.unlockUser(request, userId);
        return ResponseEntity.ok(new ApiResponse("User unlocked successfully", user));
    }

    @PutMapping("/{userId}/change-role")
    public ResponseEntity<ApiResponse> changeUserRole(
            HttpServletRequest request,
            @PathVariable Long userId,
            @RequestParam Role newRole) {

        AdminUserResponse user = adminUserManagementService.changeUserRole(request, userId, newRole);
        return ResponseEntity.ok(new ApiResponse("User role changed successfully", user));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse> getUserStatistics(HttpServletRequest request) {
        UserStatsResponse stats = adminUserManagementService.getUserStatistics(request);
        return ResponseEntity.ok(new ApiResponse("User statistics retrieved successfully", stats));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchUsers(
            HttpServletRequest request,
            @RequestParam String searchTerm) {

        List<AdminUserResponse> users = adminUserManagementService.searchUsers(request, searchTerm);
        return ResponseEntity.ok(new ApiResponse("Users search completed successfully", users));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse> getRecentlyRegisteredUsers(
            HttpServletRequest request,
            @RequestParam(defaultValue = "10") int limit) {

        List<AdminUserResponse> users = adminUserManagementService.getRecentlyRegisteredUsers(request, limit);
        return ResponseEntity.ok(new ApiResponse("Recently registered users retrieved successfully", users));
    }

    @GetMapping("/inactive")
    public ResponseEntity<ApiResponse> getInactiveUsers(
            HttpServletRequest request,
            @RequestParam(defaultValue = "30") int daysSinceLastActivity) {

        List<AdminUserResponse> users = adminUserManagementService.getInactiveUsers(request, daysSinceLastActivity);
        return ResponseEntity.ok(new ApiResponse("Inactive users retrieved successfully", users));
    }

    @DeleteMapping("/bulk-delete")
    public ResponseEntity<ApiResponse> bulkDeleteUsers(
            HttpServletRequest request,
            @RequestBody List<Long> userIds) {

        adminUserManagementService.bulkDeleteUsers(request, userIds);
        return ResponseEntity.ok(new ApiResponse("Users deleted successfully", null));
    }

    @PutMapping("/bulk/activate")
    public ResponseEntity<ApiResponse> bulkActivateUsers(
            HttpServletRequest request,
            @RequestBody List<Long> userIds) {

        adminUserManagementService.bulkActivateUsers(request, userIds);
        return ResponseEntity.ok(new ApiResponse("Users activated successfully", null));
    }

    @PutMapping("/bulk/deactivate")
    public ResponseEntity<ApiResponse> bulkDeactivateUsers(
            HttpServletRequest request,
            @RequestBody List<Long> userIds) {

        adminUserManagementService.bulkDeactivateUsers(request, userIds);
        return ResponseEntity.ok(new ApiResponse("Users deactivated successfully", null));
    }
}