package com.wms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 出库单主表
 */
@Entity
@Table(name = "outbound_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboundOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 50)
    private String orderNo;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(length = 20)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
