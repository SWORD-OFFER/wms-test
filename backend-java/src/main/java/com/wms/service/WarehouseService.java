package com.wms.service;

import com.wms.dto.LocationResponse;
import com.wms.dto.WarehouseResponse;
import com.wms.entity.Location;
import com.wms.entity.Warehouse;
import com.wms.repository.LocationRepository;
import com.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;

    public List<WarehouseResponse> listAll() {
        return warehouseRepository.findAll().stream()
                .map(w -> WarehouseResponse.builder()
                        .id(w.getId())
                        .code(w.getCode())
                        .name(w.getName())
                        .build())
                .toList();
    }

    public List<LocationResponse> getLocationsByWarehouse(Long warehouseId) {
        return locationRepository.findByWarehouseId(warehouseId).stream()
                .map(this::toLocationResponse)
                .toList();
    }

    private LocationResponse toLocationResponse(Location location) {
        return LocationResponse.builder()
                .id(location.getId())
                .warehouseId(location.getWarehouseId())
                .code(location.getCode())
                .status(location.getStatus())
                .build();
    }
}
