package com.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 出库单创建响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboundOrderCreateResponse {

    private Long id;
    private String orderNo;
    private String customerName;
    private String status;
    private List<OutboundItemResponse> items;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutboundItemResponse {
        private Long productId;
        private String productName;
        private Integer quantity;
        private String locationCode;
    }
}
