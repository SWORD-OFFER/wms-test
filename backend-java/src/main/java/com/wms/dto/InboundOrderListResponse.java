package com.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 入库单列表项响应（API_SPEC 3.2）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboundOrderListResponse {

    private Long id;
    private String orderNo;
    private String supplierName;
    private String status;
    private LocalDateTime createdAt;
}
