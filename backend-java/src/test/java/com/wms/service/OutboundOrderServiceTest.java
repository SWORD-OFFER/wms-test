package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.OutboundOrderCreateRequest;
import com.wms.dto.OutboundOrderCreateResponse;
import com.wms.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 出库单创建 + 库存扣减 单元测试（选做 A）
 * 覆盖：正常扣减 + 单号格式 + 库存不足回滚 + 库存不存在
 * 断言用"前后快照"，与用例执行顺序无关
 */
@SpringBootTest
class OutboundOrderServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    private OutboundOrderCreateRequest buildRequest(Long productId, Integer quantity, String locationCode) {
        OutboundOrderCreateRequest request = new OutboundOrderCreateRequest();
        request.setCustomerName("测试客户");
        OutboundOrderCreateRequest.OutboundItemRequest item = new OutboundOrderCreateRequest.OutboundItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setLocationCode(locationCode);
        request.setItems(List.of(item));
        return request;
    }

    @Test
    void createOutboundOrder_正常扣减_单号合规() {
        int before = inventoryRepository.findByProductIdAndLocationCode(2L, "WH-A-01-01").orElseThrow().getQuantity();

        OutboundOrderCreateResponse response = inventoryService.createOutboundOrder(buildRequest(2L, 10, "WH-A-01-01"));

        assertEquals("COMPLETED", response.getStatus());
        assertTrue(response.getOrderNo().matches("OUT-\\d{8}-\\d{3}"));
        int after = inventoryRepository.findByProductIdAndLocationCode(2L, "WH-A-01-01").orElseThrow().getQuantity();
        assertEquals(before - 10, after, "出库后库存应精确扣减");
    }

    @Test
    void createOutboundOrder_库存不足_抛异常且库存不变() {
        int before = inventoryRepository.findByProductIdAndLocationCode(3L, "WH-A-01-02").orElseThrow().getQuantity();

        assertThrows(BusinessException.class,
                () -> inventoryService.createOutboundOrder(buildRequest(3L, 9999, "WH-A-01-02")));

        int after = inventoryRepository.findByProductIdAndLocationCode(3L, "WH-A-01-02").orElseThrow().getQuantity();
        assertEquals(before, after, "库存不足时事务应回滚，库存不变");
    }

    @Test
    void createOutboundOrder_库存记录不存在_抛异常() {
        assertThrows(BusinessException.class,
                () -> inventoryService.createOutboundOrder(buildRequest(5L, 1, "WH-A-01-01")));
    }
}
