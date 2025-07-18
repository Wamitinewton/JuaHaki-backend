package com.juahaki.juahaki.dto.admin.user;

import com.juahaki.juahaki.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeUserRoleRequest {
    private Role newRole;
}