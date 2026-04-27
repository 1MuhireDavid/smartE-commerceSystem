package org.ecommerce.api.dto.response;

import org.ecommerce.api.entity.UserEntity;

public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String role;
    /** Seconds until the token expires — convenient for Postman / frontend. */
    private long expiresIn;

    public static AuthResponse from(UserEntity user, String token, long expirationMs) {
        AuthResponse r = new AuthResponse();
        r.token     = token;
        r.userId    = user.getUserId();
        r.username  = user.getUsername();
        r.email     = user.getEmail();
        r.fullName  = user.getFullName();
        r.role      = user.getRole();
        r.expiresIn = expirationMs / 1000;
        return r;
    }

    public String getToken()     { return token; }
    public String getType()      { return type; }
    public Long getUserId()      { return userId; }
    public String getUsername()  { return username; }
    public String getEmail()     { return email; }
    public String getFullName()  { return fullName; }
    public String getRole()      { return role; }
    public long getExpiresIn()   { return expiresIn; }
}
