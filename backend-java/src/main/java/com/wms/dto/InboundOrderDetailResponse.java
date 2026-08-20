package com.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 入库单详情响应（API_SPEC 3.3）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboundOrderDetailResponse {

    private Long id;
    private String orderNo;
    private String supplierName;
    private String status;
    private List<Item> items;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long productId;
        private String productName;
        private Integer quantity;
        private String locationCode;
    }
}
