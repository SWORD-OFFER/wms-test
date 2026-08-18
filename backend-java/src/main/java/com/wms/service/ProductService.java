package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.dto.ProductCreateRequest;
import com.wms.dto.ProductResponse;
import com.wms.dto.ProductUpdateRequest;
import com.wms.entity.Product;
import com.wms.repository.InventoryRepository;
import com.wms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品管理 Service — 参考实现
 * 展示了：参数校验、异常处理、事务管理、DTO 转换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public List<ProductResponse> list(String keyword) {
        List<Product> products = productRepository.search(keyword);
        return products.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("SKU已存在: " + request.getSku());
        }
        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .unit(request.getUnit() != null ? request.getUnit() : "个")
                .build();
        product = productRepository.save(product);
        log.info("创建商品成功: id={}, sku={}", product.getId(), product.getSku());
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        product.setName(request.getName());
        if (request.getUnit() != null) {
            product.setUnit(request.getUnit());
        }
        product = productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new BusinessException(404, "商品不存在");
        }
        // 修复 Bug1：删除前校验是否存在关联库存，避免留下孤立库存数据
        if (inventoryRepository.existsByProductId(id)) {
            throw new BusinessException("该商品存在关联库存，无法删除");
        }
        productRepository.deleteById(id);
        log.info("删除商品: id={}", id);
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .unit(product.getUnit())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
