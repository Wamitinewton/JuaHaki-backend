package com.juahaki.juahaki.core.user.dto.admin;

import com.juahaki.juahaki.shared.enums.AuthProvider;
import com.juahaki.juahaki.shared.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserFilterRequest {
    private Role role;
    private AuthProvider provider;
    private Boolean emailVerified;
    private Boolean isEnabled;
    private Boolean isAccountNonLocked;
    private String searchTerm;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private LocalDateTime updatedAfter;
    private LocalDateTime updatedBefore;
}