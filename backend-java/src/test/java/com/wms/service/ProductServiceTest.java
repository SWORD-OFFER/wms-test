package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.entity.Product;
import com.wms.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商品删除 Bug 修复 单元测试（任务 3）
 */
@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void delete_有关联库存_拒绝删除() {
        // 商品 1 有种子库存（WH-A-01-01）
        assertThrows(BusinessException.class, () -> productService.delete(1L));
        assertTrue(productRepository.existsById(1L), "删除被拒后商品应保留");
    }

    @Test
    void delete_无关联库存_删除成功() {
        // 新建一个无库存的商品再删除，避免依赖种子数据执行顺序
        Product p = productRepository.save(Product.builder()
                .name("临时商品")
                .sku("SKU-TMP-" + System.currentTimeMillis())
                .unit("个")
                .build());

        productService.delete(p.getId());
        assertFalse(productRepository.existsById(p.getId()), "无关联库存的商品应删除成功");
    }
}
