package com.mdsproject.backend.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ChangeRoleRequest {
    @NotNull
    private UUID userId;

    @NotBlank
    private String role; // ADMIN or MEMBER
}
