package org.ecommerce.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class UserEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    // Nullable: OAuth2 users (Google login) authenticate via their provider and have no local password.
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "role", nullable = false, length = 10)
    private String role;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider = "local";

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UserEntity() {}

    // ── UserDetails ───────────────────────────────────────────────────────────

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    /** Returns the BCrypt-hashed password; excluded from JSON responses. */
    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    /** Returns the username field — used as the JWT subject. */
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() { return true; }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() { return true; }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    @JsonIgnore
    public boolean isEnabled() { return active; }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getUserId()                            { return userId; }
    public void setUserId(Long userId)                 { this.userId = userId; }

    public void setUsername(String username)           { this.username = username; }

    public String getEmail()                           { return email; }
    public void setEmail(String email)                 { this.email = email; }

    @JsonIgnore
    public String getPasswordHash()                    { return passwordHash; }
    public void setPasswordHash(String passwordHash)   { this.passwordHash = passwordHash; }

    public String getFullName()                        { return fullName; }
    public void setFullName(String fullName)           { this.fullName = fullName; }

    public String getPhone()                           { return phone; }
    public void setPhone(String phone)                 { this.phone = phone; }

    public String getRole()                            { return role; }
    public void setRole(String role)                   { this.role = role; }

    public String getProvider()                        { return provider; }
    public void setProvider(String provider)           { this.provider = provider; }

    public String getProviderId()                      { return providerId; }
    public void setProviderId(String providerId)       { this.providerId = providerId; }

    public boolean isActive()                          { return active; }
    public void setActive(boolean active)              { this.active = active; }

    public LocalDateTime getCreatedAt()                { return createdAt; }
    public void setCreatedAt(LocalDateTime t)          { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()                { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)          { this.updatedAt = t; }
}
