package com.juahaki.juahaki.core.user.dto.admin;

import com.juahaki.juahaki.shared.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeUserRoleRequest {
    private Role newRole;
}