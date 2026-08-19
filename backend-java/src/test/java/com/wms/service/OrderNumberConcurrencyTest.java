package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.OutboundOrderCreateRequest;
import com.wms.entity.Inventory;
import com.wms.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单单号并发测试：验证 count+1 生成的单号在并发下撞唯一约束时，
 * 由 withOrderNoRetry 自动重试取下一个序号，而不是请求失败。
 * 每个线程使用不同库存行，仅让单号成为唯一竞争点。
 */
@SpringBootTest
class OrderNumberConcurrencyTest {

    @Autowired
    private OutboundOrderService outboundOrderService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("并发创建出库单：单号冲突被重试解决")
    void shouldResolveOrderNoConflictsByRetry() throws Exception {
        final int threads = 10;
        // 每个线程一个独立库存行（数量 1），避免库存行锁竞争，只竞争单号
        for (int i = 0; i < threads; i++) {
            inventoryRepository.save(Inventory.builder()
                    .productId(4L)
                    .locationCode("TEST-LOC-" + i)
                    .quantity(1)
                    .build());
        }

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                start.await();
                OutboundOrderCreateRequest request = new OutboundOrderCreateRequest();
                request.setCustomerName("并发客户");
                OutboundOrderCreateRequest.OutboundItemRequest item = new OutboundOrderCreateRequest.OutboundItemRequest();
                item.setProductId(4L);
                item.setQuantity(1);
                item.setLocationCode("TEST-LOC-" + idx);
                request.setItems(List.of(item));
                try {
                    outboundOrderService.createOutboundOrder(request);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getMessage().contains("单号冲突")) {
                        conflict.incrementAndGet();
                    }
                } catch (Exception e) {
                    conflict.incrementAndGet();
                }
                return null;
            }));
        }

        start.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(threads, success.get(), "并发创建订单应全部成功（单号冲突被重试解决）");
        assertEquals(0, conflict.get(), "单号冲突不应残留失败请求");
    }
}
