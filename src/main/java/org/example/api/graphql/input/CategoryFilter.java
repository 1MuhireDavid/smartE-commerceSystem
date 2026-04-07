package org.example.api.graphql.input;

/** Bound from the GraphQL {@code CategoryFilter} input type via {@code @Argument}. */
public class CategoryFilter {

    private String  keyword;
    private Boolean active;

    public String  getKeyword()          { return keyword; }
    public void    setKeyword(String v)  { this.keyword = v; }

    public Boolean getActive()           { return active; }
    public void    setActive(Boolean v)  { this.active = v; }
}
