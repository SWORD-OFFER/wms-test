package com.wms.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 出库单明细
 */
@Entity
@Table(name = "outbound_order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboundOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "location_code", nullable = false, length = 50)
    private String locationCode;
}
