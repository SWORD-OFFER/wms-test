package com.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 出库单创建请求
 */
@Data
public class OutboundOrderCreateRequest {

    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    @NotEmpty(message = "出库明细不能为空")
    @Valid
    private List<OutboundItemRequest> items;

    @Data
    public static class OutboundItemRequest {

        @NotNull(message = "商品ID不能为空")
        private Long productId;

        @Min(value = 1, message = "数量必须大于0")
        @Max(value = 999999, message = "数量不能超过999999")
        private Integer quantity;

        @NotBlank(message = "库位编码不能为空")
        private String locationCode;
    }
}
