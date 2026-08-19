package com.wms.service;

import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InboundOrderCreateRequest.InboundItemRequest;
import com.wms.entity.Inventory;
import com.wms.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 入库并发累加测试：验证悲观锁（findForUpdate）保证并发入库到同一库位不丢更新、不失败
 */
@SpringBootTest
class InboundConcurrencyTest {

    @Autowired
    private InboundOrderService inboundOrderService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("并发入库同一库位：不丢更新、全部成功")
    void shouldAccumulateWithoutLostUpdateUnderConcurrency() throws Exception {
        final String loc = "WH-A-02-01"; // 种子库位（存在），且商品1 在该库位无种子库存、无其他测试占用
        // 预置库存行，让并发累加落在悲观锁路径上
        inventoryRepository.save(Inventory.builder().productId(1L).locationCode(loc).quantity(0).build());

        final int threads = 10;
        final int perThread = 5; // 总期望 10 * 5 = 50
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger success = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                InboundOrderCreateRequest request = new InboundOrderCreateRequest();
                request.setSupplierName("并发供应商");
                InboundItemRequest item = new InboundItemRequest();
                item.setProductId(1L);
                item.setQuantity(perThread);
                item.setLocationCode(loc);
                request.setItems(List.of(item));
                try {
                    inboundOrderService.createInboundOrder(request);
                    success.incrementAndGet();
                } catch (Exception ignored) {
                    // 失败不计入 success，由断言兜底
                }
                return null;
            }));
        }

        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(threads, success.get(), "所有并发入库都应成功（单号重试 + 库存锁收敛）");
        int after = inventoryRepository.findByProductIdAndLocationCode(1L, loc).orElseThrow().getQuantity();
        assertEquals(threads * perThread, after, "并发累加后库存应为 50，不丢更新");
    }
}
