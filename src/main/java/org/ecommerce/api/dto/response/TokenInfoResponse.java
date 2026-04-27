package org.ecommerce.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

@Schema(description = "Decoded claims extracted from the current JWT — for verification and debugging")
public class TokenInfoResponse {

    @Schema(description = "Subject claim (username)", example = "john_doe")
    private String subject;

    @Schema(description = "userId custom claim", example = "42")
    private Long userId;

    @Schema(description = "email custom claim", example = "john@example.com")
    private String email;

    @Schema(description = "role custom claim", example = "customer")
    private String role;

    @Schema(description = "Signature algorithm (always HS256)", example = "HS256")
    private String algorithm;

    @Schema(description = "Issued-at timestamp")
    private Date issuedAt;

    @Schema(description = "Expiration timestamp")
    private Date expiration;

    @Schema(description = "Seconds remaining before the token expires", example = "86123")
    private long expiresInSeconds;

    public TokenInfoResponse() {}

    public TokenInfoResponse(String subject, Long userId, String email, String role,
                              String algorithm, Date issuedAt, Date expiration, long expiresInSeconds) {
        this.subject          = subject;
        this.userId           = userId;
        this.email            = email;
        this.role             = role;
        this.algorithm        = algorithm;
        this.issuedAt         = issuedAt;
        this.expiration       = expiration;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getSubject()          { return subject; }
    public Long getUserId()             { return userId; }
    public String getEmail()            { return email; }
    public String getRole()             { return role; }
    public String getAlgorithm()        { return algorithm; }
    public Date getIssuedAt()           { return issuedAt; }
    public Date getExpiration()         { return expiration; }
    public long getExpiresInSeconds()   { return expiresInSeconds; }
}
