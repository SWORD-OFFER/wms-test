package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InboundOrderCreateResponse;
import com.wms.service.InboundOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 入库单 Controller
 */
@RestController
@RequestMapping("/api/inbound-orders")
@RequiredArgsConstructor
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;

    @PostMapping
    public ApiResponse<InboundOrderCreateResponse> createInboundOrder(@Valid @RequestBody InboundOrderCreateRequest request) {
        return ApiResponse.created("入库单创建成功", inboundOrderService.createInboundOrder(request));
    }
}
