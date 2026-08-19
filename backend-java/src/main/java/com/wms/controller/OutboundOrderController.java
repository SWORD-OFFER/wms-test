package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.OutboundOrderCreateRequest;
import com.wms.dto.OutboundOrderCreateResponse;
import com.wms.service.OutboundOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 出库单 Controller（选做 A）
 */
@Tag(name = "出库单", description = "出库单创建接口（悲观锁防超卖）")
@RestController
@RequestMapping("/api/outbound-orders")
@RequiredArgsConstructor
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;

    @Operation(summary = "创建出库单", description = "创建出库单并扣减库存，悲观锁保证并发安全")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OutboundOrderCreateResponse> createOutboundOrder(@Valid @RequestBody OutboundOrderCreateRequest request) {
        return ApiResponse.created("出库单创建成功", outboundOrderService.createOutboundOrder(request));
    }
}
