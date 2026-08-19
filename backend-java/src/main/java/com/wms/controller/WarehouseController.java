package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.LocationResponse;
import com.wms.dto.WarehouseResponse;
import com.wms.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/warehouses")
    public ApiResponse<List<WarehouseResponse>> listWarehouses() {
        return ApiResponse.success(warehouseService.listAll());
    }

    @GetMapping("/warehouses/{id}/locations")
    public ApiResponse<List<LocationResponse>> getLocations(@PathVariable Long id) {
        return ApiResponse.success(warehouseService.getLocationsByWarehouse(id));
    }
}
