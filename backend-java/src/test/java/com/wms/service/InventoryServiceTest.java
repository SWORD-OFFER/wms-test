package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InboundOrderCreateRequest.InboundItemRequest;
import com.wms.dto.InboundOrderCreateResponse;
import com.wms.entity.Inventory;
import com.wms.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 入库单创建 Service 层单元测试（选做 B）
 * 覆盖：正常累加 + 单号格式 + 事务回滚
 */
@SpringBootTest
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    private InboundOrderCreateRequest buildRequest(Long productId, Integer quantity, String locationCode) {
        InboundOrderCreateRequest request = new InboundOrderCreateRequest();
        request.setSupplierName("测试供应商");
        InboundItemRequest item = new InboundItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setLocationCode(locationCode);
        request.setItems(List.of(item));
        return request;
    }

    @Test
    void createInboundOrder_正常入库_库存累加且单号合规() {
        // product 5（屏幕保护膜）在种子数据中无库存，结果确定
        InboundOrderCreateResponse response = inventoryService.createInboundOrder(
                buildRequest(5L, 7, "WH-B-01-01"));

        assertEquals("COMPLETED", response.getStatus());
        assertTrue(response.getOrderNo().matches("IN-\\d{8}-\\d{3}"));

        Optional<Inventory> inv = inventoryRepository.findByProductIdAndLocationCode(5L, "WH-B-01-01");
        assertTrue(inv.isPresent());
        assertEquals(7, inv.get().getQuantity());
    }

    @Test
    void createInboundOrder_库位不存在_整个事务回滚库存不变() {
        // 第一行有效（先累加库存），第二行库位不存在 → 事务整体回滚
        Inventory before = inventoryRepository.findByProductIdAndLocationCode(3L, "WH-A-01-02").orElseThrow();
        int qtyBefore = before.getQuantity();

        InboundOrderCreateRequest request = new InboundOrderCreateRequest();
        request.setSupplierName("测试供应商");
        InboundItemRequest ok = new InboundItemRequest();
        ok.setProductId(3L);
        ok.setQuantity(100);
        ok.setLocationCode("WH-A-01-02");
        InboundItemRequest bad = new InboundItemRequest();
        bad.setProductId(3L);
        bad.setQuantity(1);
        bad.setLocationCode("WH-XX-99");
        request.setItems(List.of(ok, bad));

        assertThrows(BusinessException.class, () -> inventoryService.createInboundOrder(request));

        Inventory after = inventoryRepository.findByProductIdAndLocationCode(3L, "WH-A-01-02").orElseThrow();
        assertEquals(qtyBefore, after.getQuantity(), "事务回滚后第一行已累加的库存不应保留");
    }

    @Test
    void createInboundOrder_商品不存在_抛业务异常() {
        assertThrows(BusinessException.class,
                () -> inventoryService.createInboundOrder(buildRequest(999L, 1, "WH-A-01-01")));
    }
}
