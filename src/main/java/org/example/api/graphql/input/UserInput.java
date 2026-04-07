package org.example.api.graphql.input;

import org.example.api.dto.request.UserRequest;

/** Bound from the GraphQL {@code UserInput} mutation input type via {@code @Argument}. */
public class UserInput {

    private String  username;
    private String  email;
    private String  password;
    private String  fullName;
    private String  phone;
    private String  role   = "customer";
    private boolean active = true;

    /** Converts this GraphQL input into the DTO expected by the service layer. */
    public UserRequest toRequest() {
        UserRequest req = new UserRequest();
        req.setUsername(username);
        req.setEmail(email);
        req.setPassword(password);
        req.setFullName(fullName);
        req.setPhone(phone);
        req.setRole(role);
        req.setActive(active);
        return req;
    }

    public String  getUsername()          { return username; }
    public void    setUsername(String v)  { this.username = v; }

    public String  getEmail()             { return email; }
    public void    setEmail(String v)     { this.email = v; }

    public String  getPassword()          { return password; }
    public void    setPassword(String v)  { this.password = v; }

    public String  getFullName()          { return fullName; }
    public void    setFullName(String v)  { this.fullName = v; }

    public String  getPhone()             { return phone; }
    public void    setPhone(String v)     { this.phone = v; }

    public String  getRole()              { return role; }
    public void    setRole(String v)      { this.role = v; }

    public boolean isActive()             { return active; }
    public void    setActive(boolean v)   { this.active = v; }
}
