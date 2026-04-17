package org.ecommerce.api.graphql.input;

/** Bound from the GraphQL {@code UserFilter} input type via {@code @Argument}. */
public class UserFilter {

    private String  keyword;
    private String  role;
    private Boolean active;

    public String  getKeyword()          { return keyword; }
    public void    setKeyword(String v)  { this.keyword = v; }

    public String  getRole()             { return role; }
    public void    setRole(String v)     { this.role = v; }

    public Boolean getActive()           { return active; }
    public void    setActive(Boolean v)  { this.active = v; }
}
