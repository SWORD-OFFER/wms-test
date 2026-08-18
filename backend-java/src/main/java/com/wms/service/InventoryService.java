package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.common.PageResult;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InboundOrderCreateResponse;
import com.wms.dto.InboundOrderCreateResponse.InboundItemResponse;
import com.wms.dto.InventoryResponse;
import com.wms.dto.OutboundOrderCreateRequest;
import com.wms.dto.OutboundOrderCreateResponse;
import com.wms.dto.OutboundOrderCreateResponse.OutboundItemResponse;
import com.wms.entity.InboundOrder;
import com.wms.entity.InboundOrderItem;
import com.wms.entity.Inventory;
import com.wms.entity.OutboundOrder;
import com.wms.entity.OutboundOrderItem;
import com.wms.entity.Product;
import com.wms.repository.InboundOrderItemRepository;
import com.wms.repository.InboundOrderRepository;
import com.wms.repository.InventoryRepository;
import com.wms.repository.LocationRepository;
import com.wms.repository.OutboundOrderItemRepository;
import com.wms.repository.OutboundOrderRepository;
import com.wms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final OutboundOrderRepository outboundOrderRepository;
    private final OutboundOrderItemRepository outboundOrderItemRepository;
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
     * 库存查询：按商品名/SKU 模糊搜索 + 仓库筛选 + 分页
     */
    public PageResult<InventoryResponse> queryInventory(String keyword, Long warehouseId,
                                                         int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize);
        Page<InventoryResponse> result = inventoryRepository.queryInventory(keyword, warehouseId, pageRequest);
        return new PageResult<>(result.getContent(), result.getTotalElements(), page, pageSize);
    }

    /**
     * 出库单创建 + 库存扣减（选做 A）
     * 并发安全方案：悲观锁（SELECT ... FOR UPDATE）在事务内锁定库存行，
     * 锁内校验库存充足再扣减，防止高并发超卖。
     */
    @Transactional
    public OutboundOrderCreateResponse createOutboundOrder(OutboundOrderCreateRequest request) {
        String orderNo = generateOutboundOrderNo();

        OutboundOrder order = OutboundOrder.builder()
                .orderNo(orderNo)
                .customerName(request.getCustomerName())
                .status("COMPLETED")
                .build();
        order = outboundOrderRepository.save(order);

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
    private String generateOutboundOrderNo() {
        String prefix = "OUT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long todayCount = outboundOrderRepository.countByOrderNoPrefix(prefix);
        return prefix + "-" + String.format("%03d", todayCount + 1);
    }
}
