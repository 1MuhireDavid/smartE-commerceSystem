package org.example.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.example.api.validation.ValidEnum;

@Schema(description = "Payload for creating or updating a user")
public class UserRequest {

    @Schema(description = "Unique username", example = "john_doe", maxLength = 50)
    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username must not exceed 50 characters")
    private String username;

    @Schema(description = "User email address", example = "john@example.com", maxLength = 100)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Schema(description = "Plain-text password (min 8 characters)", example = "Secret123!", minLength = 8)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Schema(description = "Full display name", example = "John Doe", maxLength = 100)
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Schema(description = "Phone number (optional)", example = "+1-555-0100", maxLength = 20)
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;

    @Schema(description = "User role", example = "customer",
            allowableValues = {"customer", "seller", "admin"}, defaultValue = "customer")
    @ValidEnum(allowed = {"customer", "seller", "admin"})
    private String role = "customer";

    @Schema(description = "Whether the account is active", defaultValue = "true")
    private boolean active = true;

    public String getUsername()          { return username; }
    public void setUsername(String v)    { this.username = v; }

    public String getEmail()             { return email; }
    public void setEmail(String v)       { this.email = v; }

    public String getPassword()          { return password; }
    public void setPassword(String v)    { this.password = v; }

    public String getFullName()          { return fullName; }
    public void setFullName(String v)    { this.fullName = v; }

    public String getPhone()             { return phone; }
    public void setPhone(String v)       { this.phone = v; }

    public String getRole()              { return role; }
    public void setRole(String v)        { this.role = v; }

    public boolean isActive()            { return active; }
    public void setActive(boolean v)     { this.active = v; }
}
