package org.ecommerce.model;

import java.time.LocalDateTime;

public class User {

    private long   userId;
    private String username;
    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private String address;
    private String role;          // customer | seller | admin
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {}

    public User(long userId, String username, String email, String passwordHash,
                String fullName, String phone, String address, String role, boolean isActive,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId       = userId;
        this.username     = username;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.fullName     = fullName;
        this.phone        = phone;
        this.address      = address;
        this.role         = role;
        this.isActive     = isActive;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
    }

    public long    getUserId()                        { return userId; }
    public void    setUserId(long userId)             { this.userId = userId; }

    public String  getUsername()                      { return username; }
    public void    setUsername(String username)       { this.username = username; }

    public String  getEmail()                         { return email; }
    public void    setEmail(String email)             { this.email = email; }

    public String  getPasswordHash()                  { return passwordHash; }
    public void    setPasswordHash(String ph)         { this.passwordHash = ph; }

    public String  getFullName()                      { return fullName; }
    public void    setFullName(String fullName)       { this.fullName = fullName; }

    public String  getPhone()                         { return phone; }
    public void    setPhone(String phone)             { this.phone = phone; }

    public String  getAddress()                       { return address; }
    public void    setAddress(String address)         { this.address = address; }

    public String  getRole()                          { return role; }
    public void    setRole(String role)               { this.role = role; }

    public boolean isActive()                         { return isActive; }
    public void    setActive(boolean active)          { this.isActive = active; }

    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void    setCreatedAt(LocalDateTime t)      { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()               { return updatedAt; }
    public void    setUpdatedAt(LocalDateTime t)      { this.updatedAt = t; }

    @Override public String toString() { return fullName + " (" + username + ")"; }
}
