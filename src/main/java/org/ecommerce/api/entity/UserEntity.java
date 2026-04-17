package org.ecommerce.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "role", nullable = false, length = 10)
    private String role;

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

    public Long getUserId()                        { return userId; }
    public void setUserId(Long userId)             { this.userId = userId; }

    public String getUsername()                    { return username; }
    public void setUsername(String username)       { this.username = username; }

    public String getEmail()                       { return email; }
    public void setEmail(String email)             { this.email = email; }

    public String getPasswordHash()                { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName()                    { return fullName; }
    public void setFullName(String fullName)       { this.fullName = fullName; }

    public String getPhone()                       { return phone; }
    public void setPhone(String phone)             { this.phone = phone; }

    public String getRole()                        { return role; }
    public void setRole(String role)               { this.role = role; }

    public boolean isActive()                      { return active; }
    public void setActive(boolean active)          { this.active = active; }

    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void setCreatedAt(LocalDateTime t)      { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()            { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)      { this.updatedAt = t; }
}
