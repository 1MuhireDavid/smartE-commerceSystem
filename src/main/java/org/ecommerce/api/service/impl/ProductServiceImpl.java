package org.ecommerce.api.service.impl;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.ProductRequest;
import org.ecommerce.api.entity.CategoryEntity;
import org.ecommerce.api.entity.InventoryEntity;
import org.ecommerce.api.entity.ProductEntity;
import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.repository.CategoryRepository;
import org.ecommerce.api.repository.ProductRepository;
import org.ecommerce.api.repository.UserRepository;
import org.ecommerce.api.service.ProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository  productRepository;
    private final UserRepository     userRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              UserRepository userRepository,
                              CategoryRepository categoryRepository) {
        this.productRepository  = productRepository;
        this.userRepository     = userRepository;
        this.categoryRepository = categoryRepository;
    }

    // Cache browse (no keyword) page results — repeated page views served from Caffeine O(1)
    // instead of a full DB query.  Keyword searches are not cached: they are low-repetition
    // and the GIN index already makes them fast.
    // Cache key encodes all filter dimensions + pagination so different pages don't collide.
    @Override
    @Cacheable(value = "productLists",
               key   = "T(String).valueOf(#categoryId) + ':' + #status + ':' + T(String).valueOf(#sellerId) + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
               condition = "#keyword == null")
    public PagedResponse<ProductEntity> findAll(
            String keyword, Integer categoryId, String status, Long sellerId, Pageable pageable) {
        Page<ProductEntity> page =
                productRepository.searchFts(keyword, categoryId, status, sellerId, pageable);
        return PagedResponse.of(page);
    }

    // findByIdWithAssociations() loads category + seller + inventory via LEFT JOIN FETCH in a
    // single query, replacing up to 3 separate lazy SELECT statements.
    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductEntity findById(long id) {
        return productRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found with id: " + id));
    }

    // create() never needs to evict individual product entries (the new product has no
    // cached entry yet).  Only the list cache is stale after a new product is added.
    @Override
    @Transactional
    @CacheEvict(value = "productLists", allEntries = true)
    public ProductEntity create(ProductRequest request) {
        if (productRepository.existsBySlug(request.getSlug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Slug already in use: " + request.getSlug());
        }

        ProductEntity product = buildProduct(new ProductEntity(), request);

        InventoryEntity inventory = new InventoryEntity();
        inventory.setQtyInStock(request.getStockQuantity());
        inventory.setReservedQty(0);
        inventory.setReorderLevel(request.getReorderLevel());
        inventory.setProduct(product);
        product.setInventory(inventory);

        return productRepository.save(product);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products",     key       = "#id"),
        @CacheEvict(value = "productLists", allEntries = true)
    })
    public ProductEntity update(long id, ProductRequest request) {
        ProductEntity product = findById(id);

        if (!product.getSlug().equals(request.getSlug())
                && productRepository.existsBySlug(request.getSlug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Slug already in use: " + request.getSlug());
        }

        buildProduct(product, request);

        if (product.getInventory() != null) {
            product.getInventory().setQtyInStock(request.getStockQuantity());
            product.getInventory().setReorderLevel(request.getReorderLevel());
        }

        return productRepository.save(product);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products",     key       = "#id"),
        @CacheEvict(value = "productLists", allEntries = true)
    })
    public void delete(long id) {
        ProductEntity product = findById(id);
        productRepository.delete(product);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ProductEntity buildProduct(ProductEntity product, ProductRequest req) {
        UserEntity seller = userRepository.findById(req.getSellerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Seller not found with id: " + req.getSellerId()));

        product.setName(req.getName());
        product.setSlug(req.getSlug());
        product.setDescription(req.getDescription());
        product.setBasePrice(req.getBasePrice());
        product.setDiscountPrice(req.getDiscountPrice());
        product.setStatus(req.getStatus() != null ? req.getStatus() : "draft");
        product.setSeller(seller);

        if (req.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Category not found with id: " + req.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        return product;
    }
}
