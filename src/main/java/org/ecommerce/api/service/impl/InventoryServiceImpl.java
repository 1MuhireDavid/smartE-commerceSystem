package org.ecommerce.api.service.impl;

import org.ecommerce.api.entity.InventoryEntity;
import org.ecommerce.api.repository.InventoryRepository;
import org.ecommerce.api.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
