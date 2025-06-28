package com.juahaki.juahaki.controller.user;

import com.juahaki.juahaki.dto.user.UpdatePasswordRequest;
import com.juahaki.juahaki.dto.user.UpdateProfileRequest;
import com.juahaki.juahaki.dto.user.UserInfo;
import com.juahaki.juahaki.response.ApiResponse;
import com.juahaki.juahaki.service.user.IUserManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class UserManagementController {

    private final IUserManagementService userManagementService;

    @GetMapping("/get-profile")
    public ResponseEntity<ApiResponse> getUserProfile(HttpServletRequest request) {
        UserInfo userInfo = userManagementService.getUserById(request);
        return ResponseEntity.ok(new ApiResponse("User profile retrieved successfully", userInfo));
    }

    @PutMapping("/update-profile")
    public ResponseEntity<ApiResponse> updateProfile(
            HttpServletRequest request,
            @RequestBody UpdateProfileRequest updateProfileRequest) {

        UserInfo updatedUser = userManagementService.updateProfile(request, updateProfileRequest);
        return ResponseEntity.ok(new ApiResponse("Profile updated successfully", updatedUser));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse> updatePassword(
            HttpServletRequest request,
            @Valid @RequestBody UpdatePasswordRequest updatePasswordRequest) {

        UserInfo updatedUser = userManagementService.updatePassword(request, updatePasswordRequest);
        return ResponseEntity.ok(new ApiResponse("Password updated successfully", updatedUser));
    }

    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse> deleteAccount(HttpServletRequest request) {
        userManagementService.deleteUser(request);
        return ResponseEntity.ok(new ApiResponse("Account deleted successfully", null));
    }

    @PostMapping("/password/reset/initiate")
    public ResponseEntity<ApiResponse> initiatePasswordReset(@RequestParam String email) {
        userManagementService.initiatePasswordReset(email);
        return ResponseEntity.ok(new ApiResponse("Password reset initiated successfully", null));
    }

    @PostMapping("/password/reset/confirm")
    public ResponseEntity<ApiResponse> resetPassword(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword) {

        userManagementService.resetPassword(email, otp, newPassword);
        return ResponseEntity.ok(new ApiResponse("Password reset completed successfully", null));
    }
}