package com.mdsproject.backend.dto.group;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinGroupRequest {
    @NotBlank
    private String inviteCode;
}
