package com.wms.service;

import com.wms.common.PageResult;
import com.wms.dto.InventoryResponse;
import com.wms.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 库存查询业务
 */
@Service
@RequiredArgsConstructor
public class InventoryQueryService {

    private final InventoryRepository inventoryRepository;

    /**
     * 库存查询：按商品名/SKU 模糊搜索 + 仓库筛选 + 分页
     */
    public PageResult<InventoryResponse> queryInventory(String keyword, Long warehouseId,
                                                        int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize);
        Page<InventoryResponse> result = inventoryRepository.queryInventory(keyword, warehouseId, pageRequest);
        return new PageResult<>(result.getContent(), result.getTotalElements(), page, pageSize);
    }
}
