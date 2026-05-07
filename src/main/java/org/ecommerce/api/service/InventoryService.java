package org.ecommerce.api.service;

import org.ecommerce.api.entity.InventoryEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface InventoryService {

    List<InventoryEntity> findLowStock();

    CompletableFuture<List<InventoryEntity>> findLowStockAsync();
}
