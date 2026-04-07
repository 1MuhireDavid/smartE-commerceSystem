package org.example.api.graphql.input;

/** Bound from the GraphQL {@code ProductFilter} input type via {@code @Argument}. */
public class ProductFilter {

    private String  keyword;
    private Integer categoryId;
    private String  status;
    private Long    sellerId;

    public String  getKeyword()             { return keyword; }
    public void    setKeyword(String v)     { this.keyword = v; }

    public Integer getCategoryId()          { return categoryId; }
    public void    setCategoryId(Integer v) { this.categoryId = v; }

    public String  getStatus()              { return status; }
    public void    setStatus(String v)      { this.status = v; }

    public Long    getSellerId()            { return sellerId; }
    public void    setSellerId(Long v)      { this.sellerId = v; }
}
