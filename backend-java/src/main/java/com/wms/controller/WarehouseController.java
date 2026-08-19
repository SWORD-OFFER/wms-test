package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.LocationResponse;
import com.wms.dto.WarehouseResponse;
import com.wms.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "仓库 & 库位", description = "仓库与库位基础数据接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "仓库列表")
    @GetMapping("/warehouses")
    public ApiResponse<List<WarehouseResponse>> listWarehouses() {
        return ApiResponse.success(warehouseService.listAll());
    }

    @Operation(summary = "某仓库下的库位列表")
    @GetMapping("/warehouses/{id}/locations")
    public ApiResponse<List<LocationResponse>> getLocations(@PathVariable Long id) {
        return ApiResponse.success(warehouseService.getLocationsByWarehouse(id));
    }
}
