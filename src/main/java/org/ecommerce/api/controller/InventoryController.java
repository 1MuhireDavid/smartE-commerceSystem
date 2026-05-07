package org.ecommerce.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ecommerce.api.entity.InventoryEntity;
import org.ecommerce.api.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Tag(name = "Inventory", description = "Inventory monitoring endpoints")
@PreAuthorize("hasAnyRole('SELLER','ADMIN')")
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(
            summary     = "List low-stock items (async)",
            description = "Returns all inventory records where qty_in_stock <= reorder_level. "
                        + "The DB query runs on the async task executor so the Servlet thread is "
                        + "released immediately, allowing Tomcat to serve other requests while "
                        + "this query completes.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Low-stock items retrieved successfully"))
    @GetMapping("/low-stock")
    public CompletableFuture<ResponseEntity<org.ecommerce.api.dto.ApiResponse<List<InventoryEntity>>>> getLowStock() {
        return inventoryService.findLowStockAsync()
                .thenApply(items -> ResponseEntity.ok(
                        org.ecommerce.api.dto.ApiResponse.success(
                                "Low-stock items retrieved successfully", items)));
    }
}
