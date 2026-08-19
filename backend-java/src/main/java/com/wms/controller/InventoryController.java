package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.common.PageResult;
import com.wms.dto.InventoryResponse;
import com.wms.service.InventoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存查询 Controller
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryQueryService inventoryQueryService;

    @GetMapping
    public ApiResponse<PageResult<InventoryResponse>> queryInventory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(inventoryQueryService.queryInventory(keyword, warehouseId, page, pageSize));
    }
}
