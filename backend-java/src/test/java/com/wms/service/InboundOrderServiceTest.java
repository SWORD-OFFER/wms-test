package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.common.PageResult;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InboundOrderCreateRequest.InboundItemRequest;
import com.wms.dto.InboundOrderCreateResponse;
import com.wms.dto.InboundOrderDetailResponse;
import com.wms.dto.InboundOrderListResponse;
import com.wms.entity.Inventory;
import com.wms.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 入库单 Service 层单元测试（选做 B）
 * 覆盖：创建/累加/单号/回滚 + 列表/详情
 */
@SpringBootTest
class InboundOrderServiceTest {

    @Autowired
    private InboundOrderService inboundOrderService;

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
    @DisplayName("正常入库：库存累加且单号合规")
    void shouldIncrementStockAndGenerateValidOrderNo() {
        // 快照式断言，与用例执行顺序无关（其他用例也可能累加同一库位）
        int before = inventoryRepository.findByProductIdAndLocationCode(5L, "WH-B-01-01")
                .map(Inventory::getQuantity).orElse(0);

        InboundOrderCreateResponse response = inboundOrderService.createInboundOrder(
                buildRequest(5L, 7, "WH-B-01-01"));

        assertEquals("COMPLETED", response.getStatus());
        assertTrue(response.getOrderNo().matches("IN-\\d{8}-\\d{3}"));

        Optional<Inventory> inv = inventoryRepository.findByProductIdAndLocationCode(5L, "WH-B-01-01");
        assertTrue(inv.isPresent());
        assertEquals(before + 7, inv.get().getQuantity());
    }

    @Test
    @DisplayName("库位不存在：整个事务回滚，库存不变")
    void shouldRollbackStockChangeWhenLocationNotExist() {
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

        assertThrows(BusinessException.class, () -> inboundOrderService.createInboundOrder(request));

        Inventory after = inventoryRepository.findByProductIdAndLocationCode(3L, "WH-A-01-02").orElseThrow();
        assertEquals(qtyBefore, after.getQuantity(), "事务回滚后第一行已累加的库存不应保留");
    }

    @Test
    @DisplayName("商品不存在：抛业务异常")
    void shouldThrowWhenProductNotExist() {
        assertThrows(BusinessException.class,
                () -> inboundOrderService.createInboundOrder(buildRequest(999L, 1, "WH-A-01-01")));
    }

    @Test
    @DisplayName("入库单列表：包含刚创建的订单")
    void shouldListCreatedOrders() {
        InboundOrderCreateResponse created = inboundOrderService.createInboundOrder(
                buildRequest(5L, 3, "WH-B-01-01"));

        PageResult<InboundOrderListResponse> page = inboundOrderService.list(1, 20);
        assertTrue(page.getTotal() >= 1);
        assertTrue(page.getList().stream().anyMatch(o -> o.getOrderNo().equals(created.getOrderNo())));
        assertEquals("COMPLETED", page.getList().stream()
                .filter(o -> o.getOrderNo().equals(created.getOrderNo()))
                .findFirst().orElseThrow().getStatus());
    }

    @Test
    @DisplayName("入库单详情：含明细与商品名")
    void shouldGetDetailWithItems() {
        InboundOrderCreateResponse created = inboundOrderService.createInboundOrder(
                buildRequest(5L, 9, "WH-B-01-01"));

        InboundOrderDetailResponse detail = inboundOrderService.getById(created.getId());
        assertEquals(created.getOrderNo(), detail.getOrderNo());
        assertEquals(1, detail.getItems().size());
        assertEquals("屏幕保护膜", detail.getItems().get(0).getProductName());
        assertEquals(9, detail.getItems().get(0).getQuantity());
    }

    @Test
    @DisplayName("入库单详情：不存在抛 404")
    void shouldThrow404WhenOrderNotExist() {
        BusinessException e = assertThrows(BusinessException.class, () -> inboundOrderService.getById(99999L));
        assertEquals(404, e.getCode());
    }
}
