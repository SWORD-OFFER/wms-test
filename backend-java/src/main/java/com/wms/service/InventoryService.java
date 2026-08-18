package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InboundOrderCreateResponse;
import com.wms.dto.InboundOrderCreateResponse.InboundItemResponse;
import com.wms.dto.InventoryResponse;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.Inventory;
import com.wms.entity.Product;
import com.wms.repository.InboundOrderItemRepository;
import com.wms.repository.InboundOrderRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.LocationRepository;
import com.wms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 库存 & 入库业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderItemRepository inboundOrderItemRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    /**
     * 入库单创建
     * 事务保证：入库单主表 + 明细 + 库存累加 要么全部成功，要么全部回滚
     */
    @Transactional
    public InboundOrderCreateResponse createInboundOrder(InboundOrderCreateRequest request) {
        String orderNo = generateOrderNo();

        InboundOrder order = InboundOrder.builder()
                .orderNo(orderNo)
                .supplierName(request.getSupplierName())
                .status("COMPLETED")
                .build();
        order = inboundOrderRepository.save(order);

        List<InboundItemResponse> itemResponses = new ArrayList<>();
        for (InboundOrderCreateRequest.InboundItemRequest item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException("商品不存在: id=" + item.getProductId()));

            if (!locationRepository.existsByCode(item.getLocationCode())) {
                throw new BusinessException("库位不存在: " + item.getLocationCode());
            }

            inboundOrderItemRepository.save(InboundOrderItem.builder()
                    .orderId(order.getId())
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .locationCode(item.getLocationCode())
                    .build());

            // 库存累加（存在则累加，不存在则新建，表唯一约束 uk_product_location 兜底）
            Inventory inventory = inventoryRepository
                    .findByProductIdAndLocationCode(item.getProductId(), item.getLocationCode())
                    .orElseGet(() -> Inventory.builder()
                            .productId(item.getProductId())
                            .locationCode(item.getLocationCode())
                            .quantity(0)
                            .build());
            inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
            inventoryRepository.save(inventory);

            itemResponses.add(InboundItemResponse.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(item.getQuantity())
                    .locationCode(item.getLocationCode())
                    .build());
        }

        log.info("创建入库单成功: orderNo={}, itemCount={}", order.getOrderNo(), request.getItems().size());
        return InboundOrderCreateResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .supplierName(order.getSupplierName())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }

    /**
     * 生成入库单号：IN-YYYYMMDD-XXX（XXX 为当天递增序号）
     * 注意：count+1 在极端并发下可能产生重复单号，生产环境可依赖唯一索引 + 重试或 DB sequence 兜底
     */
    private String generateOrderNo() {
        String prefix = "IN-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long todayCount = inboundOrderRepository.countByOrderNoPrefix(prefix);
        return prefix + "-" + String.format("%03d", todayCount + 1);
    }

    /**
     * 库存查询 — 见任务2实现
     */
    public List<InventoryResponse> queryInventory(String keyword, Long warehouseId,
                                                   int page, int pageSize) {
        // TODO: 候选人实现（Phase 2）
        throw new UnsupportedOperationException("请实现库存查询功能（任务2）");
    }
}
