package com.wms.controller;

import com.wms.common.ApiResponse;
import com.wms.dto.InboundOrderCreateRequest;
import com.wms.dto.InboundOrderCreateResponse;
import com.wms.service.InboundOrderService;
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
 * 入库单 Controller
 */
@Tag(name = "入库单", description = "入库单创建接口")
@RestController
@RequestMapping("/api/inbound-orders")
@RequiredArgsConstructor
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;

    @Operation(summary = "创建入库单", description = "创建入库单并累加对应库位库存，事务保证一致性")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InboundOrderCreateResponse> createInboundOrder(@Valid @RequestBody InboundOrderCreateRequest request) {
        return ApiResponse.created("入库单创建成功", inboundOrderService.createInboundOrder(request));
    }
}
