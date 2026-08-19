package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.OutboundOrderCreateRequest;
import com.wms.dto.OutboundOrderCreateResponse;
import com.wms.service.OutboundOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 出库单 Controller（选做 A）
 */
@RestController
@RequestMapping("/api/outbound-orders")
@RequiredArgsConstructor
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;

    @PostMapping
    public ApiResponse<OutboundOrderCreateResponse> createOutboundOrder(@Valid @RequestBody OutboundOrderCreateRequest request) {
        return ApiResponse.created("出库单创建成功", outboundOrderService.createOutboundOrder(request));
    }
}
