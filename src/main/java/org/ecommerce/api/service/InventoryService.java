package org.ecommerce.api.service;

import org.ecommerce.api.entity.InventoryEntity;

import java.util.List;

public interface InventoryService {

    List<InventoryEntity> findLowStock();
}
