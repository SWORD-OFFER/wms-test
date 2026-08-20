package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.common.PageResult;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InboundOrderCreateResponse;
import com.wms.dto.InboundOrderCreateResponse.InboundItemResponse;
import com.wms.dto.InboundOrderDetailResponse;
import com.wms.dto.InboundOrderListResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 入库单业务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboundOrderService {

    private final OrderNumberService orderNumberService;
    private final InboundOrderRepository inboundOrderRepository;
    private final InboundOrderItemRepository inboundOrderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;

    /**
     * 入库单创建
     * 事务保证：入库单主表 + 明细 + 库存累加 要么全部成功，要么全部回滚。
     * 并发修复：单号冲突时由 orderNumberService 整体重试取下一个序号。
     */
    public InboundOrderCreateResponse createInboundOrder(InboundOrderCreateRequest request) {
        return orderNumberService.withOrderNoRetry("创建入库单", () -> doCreateInboundOrder(request));
    }

    private InboundOrderCreateResponse doCreateInboundOrder(InboundOrderCreateRequest request) {
        String orderNo = generateOrderNo();

        InboundOrder order = InboundOrder.builder()
                .orderNo(orderNo)
                .supplierName(request.getSupplierName())
                .status("COMPLETED")
                .build();
        // saveAndFlush：立即 flush，让单号唯一约束冲突在此抛出，交给 orderNumberService 重试
        order = inboundOrderRepository.saveAndFlush(order);

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

            // 库存累加：已存在行用悲观锁串行累加（与出库一致，避免并发丢更新）；
            // 首次创建时 findForUpdate 不返回行，靠表唯一约束 uk_product_location + orderNumberService 重试收敛
            Inventory inventory = inventoryRepository
                    .findForUpdate(item.getProductId(), item.getLocationCode())
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
     * 入库单列表（API_SPEC 3.2）
     */
    public PageResult<InboundOrderListResponse> list(int page, int pageSize) {
        Page<InboundOrder> orders = inboundOrderRepository.findAll(PageRequest.of(page - 1, pageSize));
        List<InboundOrderListResponse> list = orders.getContent().stream()
                .map(o -> InboundOrderListResponse.builder()
                        .id(o.getId())
                        .orderNo(o.getOrderNo())
                        .supplierName(o.getSupplierName())
                        .status(o.getStatus())
                        .createdAt(o.getCreatedAt())
                        .build())
                .toList();
        return new PageResult<>(list, orders.getTotalElements(), page, pageSize);
    }

    /**
     * 入库单详情（API_SPEC 3.3）
     */
    public InboundOrderDetailResponse getById(Long id) {
        InboundOrder order = inboundOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "入库单不存在: id=" + id));
        List<InboundOrderDetailResponse.Item> items = inboundOrderItemRepository.findByOrderId(order.getId())
                .stream()
                .map(item -> InboundOrderDetailResponse.Item.builder()
                        .productId(item.getProductId())
                        .productName(productRepository.findById(item.getProductId())
                                .map(Product::getName)
                                .orElse(null))
                        .quantity(item.getQuantity())
                        .locationCode(item.getLocationCode())
                        .build())
                .toList();
        return InboundOrderDetailResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .supplierName(order.getSupplierName())
                .status(order.getStatus())
                .items(items)
                .createdAt(order.getCreatedAt())
                .build();
    }

    /**
     * 生成入库单号：IN-YYYYMMDD-XXX（XXX 为当天递增序号）
     */
    private String generateOrderNo() {
        String prefix = "IN-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long todayCount = inboundOrderRepository.countByOrderNoPrefix(prefix);
        return prefix + "-" + String.format("%03d", todayCount + 1);
    }
}
