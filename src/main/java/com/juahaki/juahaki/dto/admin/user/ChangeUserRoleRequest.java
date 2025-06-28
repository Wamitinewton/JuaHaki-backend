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
@AllArgsConstructor
@NoArgsConstructor
public class ChangeUserRoleRequest {
    private Role newRole;
}