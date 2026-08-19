package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.OutboundOrderCreateRequest;
import com.wms.dto.OutboundOrderCreateResponse;
import com.wms.dto.OutboundOrderCreateResponse.OutboundItemResponse;
import com.wms.entity.Inventory;
import com.wms.entity.OutboundOrder;
import com.wms.entity.OutboundOrderItem;
import com.wms.entity.Product;
import com.wms.repository.InventoryRepository;
import com.wms.repository.OutboundOrderItemRepository;
import com.wms.repository.OutboundOrderRepository;
import com.wms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 出库单业务（选做 A）
 * 并发安全方案：悲观锁（SELECT ... FOR UPDATE）在事务内锁定库存行，
 * 锁内校验库存充足再扣减，防止高并发超卖；单号冲突时自动重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboundOrderService {

    private final OrderNumberService orderNumberService;
    private final OutboundOrderRepository outboundOrderRepository;
    private final OutboundOrderItemRepository outboundOrderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public OutboundOrderCreateResponse createOutboundOrder(OutboundOrderCreateRequest request) {
        return orderNumberService.withOrderNoRetry("创建出库单", () -> doCreateOutboundOrder(request));
    }

    private OutboundOrderCreateResponse doCreateOutboundOrder(OutboundOrderCreateRequest request) {
        String orderNo = generateOrderNo();

        OutboundOrder order = OutboundOrder.builder()
                .orderNo(orderNo)
                .customerName(request.getCustomerName())
                .status("COMPLETED")
                .build();
        // saveAndFlush：立即 flush，让单号唯一约束冲突在此抛出，交给 orderNumberService 重试
        order = outboundOrderRepository.saveAndFlush(order);

        List<OutboundItemResponse> itemResponses = new ArrayList<>();
        for (OutboundOrderCreateRequest.OutboundItemRequest item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException("商品不存在: id=" + item.getProductId()));

            // 悲观锁锁定库存行，锁内校验 + 扣减（锁持有到事务提交才释放）
            Inventory inventory = inventoryRepository
                    .findForUpdate(item.getProductId(), item.getLocationCode())
                    .orElseThrow(() -> new BusinessException(
                            "库存不存在: product=" + item.getProductId() + ", location=" + item.getLocationCode()));

            if (inventory.getQuantity() < item.getQuantity()) {
                throw new BusinessException("库存不足: 商品 " + product.getName()
                        + " 当前库存 " + inventory.getQuantity() + "，需扣减 " + item.getQuantity());
            }

            inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
            inventoryRepository.save(inventory);

            outboundOrderItemRepository.save(OutboundOrderItem.builder()
                    .orderId(order.getId())
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .locationCode(item.getLocationCode())
                    .build());

            itemResponses.add(OutboundItemResponse.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(item.getQuantity())
                    .locationCode(item.getLocationCode())
                    .build());
        }

        log.info("创建出库单成功: orderNo={}, itemCount={}", order.getOrderNo(), request.getItems().size());
        return OutboundOrderCreateResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .customerName(order.getCustomerName())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }

    /**
     * 生成出库单号：OUT-YYYYMMDD-XXX
     */
    private String generateOrderNo() {
        String prefix = "OUT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long todayCount = outboundOrderRepository.countByOrderNoPrefix(prefix);
        return prefix + "-" + String.format("%03d", todayCount + 1);
    }
}
