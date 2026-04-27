package org.ecommerce.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials for logging in (username or email accepted)")
public class LoginRequest {

    @Schema(description = "Username or email address", example = "john_doe")
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @Schema(description = "Account password", example = "Secret123!")
    @NotBlank(message = "Password is required")
    private String password;

    public String getUsernameOrEmail()          { return usernameOrEmail; }
    public void setUsernameOrEmail(String v)    { this.usernameOrEmail = v; }

    public String getPassword()                 { return password; }
    public void setPassword(String v)           { this.password = v; }
}
