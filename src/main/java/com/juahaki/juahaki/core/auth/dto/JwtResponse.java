package com.juahaki.juahaki.core.auth.dto;

import com.juahaki.juahaki.core.user.dto.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private UserInfo user;

    @Builder.Default
    private String tokenType = "Bearer";

}
