package com.wms.service;

import com.wms.common.PageResult;
import com.wms.dto.InventoryResponse;
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
    void queryInventory_关键词命中() {
        PageResult<InventoryResponse> result = inventoryQueryService.queryInventory("蓝牙", null, 1, 100);
        assertTrue(result.getTotal() > 0);
        assertTrue(result.getList().stream()
                .allMatch(r -> r.getProductName().contains("蓝牙") || r.getSku().contains("蓝牙")));
    }

    @Test
    void queryInventory_关键词未命中() {
        PageResult<InventoryResponse> result = inventoryQueryService.queryInventory("不存在的商品XYZ", null, 1, 20);
        assertEquals(0, result.getTotal());
    }

    @Test
    void queryInventory_仓库筛选_仅返回该仓库数据() {
        PageResult<InventoryResponse> result = inventoryQueryService.queryInventory(null, 1L, 1, 100);
        assertTrue(result.getTotal() > 0);
        assertTrue(result.getList().stream().allMatch(r -> "广州主仓".equals(r.getWarehouseName())));
    }

    @Test
    void queryInventory_分页_总数一致且不超页大小() {
        PageResult<InventoryResponse> full = inventoryQueryService.queryInventory(null, null, 1, 100);
        PageResult<InventoryResponse> paged = inventoryQueryService.queryInventory(null, null, 1, 2);

        assertEquals(full.getTotal(), paged.getTotal(), "分页 total 应与全量一致");
        assertTrue(paged.getList().size() <= 2);
        assertEquals(1, paged.getPage());
    }
}
