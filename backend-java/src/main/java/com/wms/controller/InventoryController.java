package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.common.PageResult;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InboundOrderCreateResponse;
import com.wms.dto.InventoryResponse;
import com.wms.dto.OutboundOrderCreateRequest;
import com.wms.dto.OutboundOrderCreateResponse;
import com.wms.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存 & 入库 Controller
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 创建入库单
     */
    @PostMapping("/inbound-orders")
    public ApiResponse<InboundOrderCreateResponse> createInboundOrder(@Valid @RequestBody InboundOrderCreateRequest request) {
        return ApiResponse.created("入库单创建成功", inventoryService.createInboundOrder(request));
    }

    /**
     * 创建出库单（选做 A）
     */
    @PostMapping("/outbound-orders")
    public ApiResponse<OutboundOrderCreateResponse> createOutboundOrder(@Valid @RequestBody OutboundOrderCreateRequest request) {
        return ApiResponse.created("出库单创建成功", inventoryService.createOutboundOrder(request));
    }

    /**
     * 库存查询
     */
    @GetMapping("/inventory")
    public ApiResponse<PageResult<InventoryResponse>> queryInventory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(inventoryService.queryInventory(keyword, warehouseId, page, pageSize));
    }
}
