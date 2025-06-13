package com.juahaki.juahaki.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NotBlank(message = "Password is required")
public class SignUpRequest {

    private String firstName;

    private String lastName;

    private String username;

    private String email;

    private String password;
}

