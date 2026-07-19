package com.argent.module.organization.dto;

import com.argent.module.auth.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddMemberRequest(
    @NotBlank @Email String email,
    @NotNull User.Role role,
    @NotBlank @Size(min = 8, max = 128) String password
) {
    public AddMemberRequest {
        if (role == User.Role.OWNER) {
            throw new IllegalArgumentException("Cannot assign OWNER role via invitation");
        }
    }
}
