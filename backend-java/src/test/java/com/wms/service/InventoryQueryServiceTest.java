package com.wms.service;

import com.wms.common.PageResult;
import com.wms.dto.InventoryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 库存查询 单元测试（任务 2）
 * 断言用相对量/过滤命中校验，与用例执行顺序无关
 */
@SpringBootTest
class InventoryQueryServiceTest {

    @Autowired
    private InventoryQueryService inventoryQueryService;

    @Test
    @DisplayName("关键词命中")
    void shouldMatchByKeyword() {
        PageResult<InventoryResponse> result = inventoryQueryService.queryInventory("蓝牙", null, null, 1, 100);
        assertTrue(result.getTotal() > 0);
        assertTrue(result.getList().stream()
                .allMatch(r -> r.getProductName().contains("蓝牙") || r.getSku().contains("蓝牙")));
    }

    @Test
    @DisplayName("关键词未命中")
    void shouldReturnEmptyWhenKeywordNoMatch() {
        PageResult<InventoryResponse> result = inventoryQueryService.queryInventory("不存在的商品XYZ", null, null, 1, 20);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("按仓库筛选：仅返回该仓库数据")
    void shouldFilterByWarehouse() {
        PageResult<InventoryResponse> result = inventoryQueryService.queryInventory(null, 1L, null, 1, 100);
        assertTrue(result.getTotal() > 0);
        assertTrue(result.getList().stream().allMatch(r -> "广州主仓".equals(r.getWarehouseName())));
    }

    @Test
    @DisplayName("按库位编码筛选：仅返回该库位数据")
    void shouldFilterByLocationCode() {
        PageResult<InventoryResponse> result = inventoryQueryService.queryInventory(null, null, "WH-A-01-01", 1, 100);
        assertTrue(result.getTotal() > 0);
        assertTrue(result.getList().stream().allMatch(r -> "WH-A-01-01".equals(r.getLocationCode())));
    }

    @Test
    @DisplayName("分页：总数一致且不超页大小")
    void shouldPaginateCorrectly() {
        PageResult<InventoryResponse> full = inventoryQueryService.queryInventory(null, null, null, 1, 100);
        PageResult<InventoryResponse> paged = inventoryQueryService.queryInventory(null, null, null, 1, 2);

        assertEquals(full.getTotal(), paged.getTotal(), "分页 total 应与全量一致");
        assertTrue(paged.getList().size() <= 2);
        assertEquals(1, paged.getPage());
    }
}
