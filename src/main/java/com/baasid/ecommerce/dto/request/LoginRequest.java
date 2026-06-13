package com.baasid.ecommerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @Schema(description = "使用者名稱", example = "admin")
    @NotBlank
    private String username;

    @Schema(description = "密碼", example = "admin123")
    @NotBlank
    private String password;
}
