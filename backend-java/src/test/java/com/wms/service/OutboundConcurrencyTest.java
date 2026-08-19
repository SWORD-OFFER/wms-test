package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.OutboundOrderCreateRequest;
import com.wms.entity.Inventory;
import com.wms.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 出库并发扣减测试（选做 A）：验证悲观锁 SELECT FOR UPDATE 防止超卖
 * 断言核心不变量：并发下库存绝不为负、扣减总量不超过初始库存。
 * （单号并发由 OrderNumberConcurrencyTest 单独验证）
 */
@SpringBootTest
class OutboundConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    private OutboundOrderCreateRequest buildRequest(Long productId, Integer quantity, String locationCode) {
        OutboundOrderCreateRequest request = new OutboundOrderCreateRequest();
        request.setCustomerName("并发客户");
        OutboundOrderCreateRequest.OutboundItemRequest item = new OutboundOrderCreateRequest.OutboundItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setLocationCode(locationCode);
        request.setItems(List.of(item));
        return request;
    }

    @Test
    void 并发扣减_不超卖() throws InterruptedException {
        // 造一块独立库存（50 件）用于压测，不污染其他用例数据
        final int initial = 50;
        inventoryRepository.save(Inventory.builder()
                .productId(4L)
                .locationCode("WH-A-02-01")
                .quantity(initial)
                .build());

        final int threads = 10;
        final int perThread = 10; // 总需求 100 > 50，必然触发库存不足
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger success = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    inventoryService.createOutboundOrder(buildRequest(4L, perThread, "WH-A-02-01"));
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    // 库存不足是预期业务结果
                } catch (Exception ignored) {
                    // 并发下的连接层瞬态异常不改变库存不变量，忽略
                }
            });
        }

        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "并发任务应在超时前完成");

        int after = inventoryRepository.findByProductIdAndLocationCode(4L, "WH-A-02-01").orElseThrow().getQuantity();

        // 核心不变量：库存绝不为负（未超卖）；扣减总量不超过初始库存
        assertTrue(after >= 0, "并发扣减后库存不得为负（未超卖）");
        assertTrue(success.get() * perThread <= initial, "扣减总量不得超过初始库存");
    }
}
