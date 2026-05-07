package org.ecommerce.api.service.impl;

import org.ecommerce.api.config.AsyncConfig;
import org.ecommerce.api.entity.InventoryEntity;
import org.ecommerce.api.repository.InventoryRepository;
import org.ecommerce.api.service.InventoryService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public List<InventoryEntity> findLowStock() {
        return inventoryRepository.findLowStock();
    }

    @Override
    @Async(AsyncConfig.EXECUTOR_BEAN)
    public CompletableFuture<List<InventoryEntity>> findLowStockAsync() {
        return CompletableFuture.completedFuture(inventoryRepository.findLowStock());
    }
}
