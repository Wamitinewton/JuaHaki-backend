package com.juahaki.juahaki.dto.admin.user;

import com.juahaki.juahaki.enums.AuthProvider;
import com.juahaki.juahaki.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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