package com.wms.service;

import com.wms.common.BusinessException;
import com.wms.entity.Product;
import com.wms.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("有关联库存：拒绝删除")
    void shouldRejectDeleteWhenInventoryExists() {
        // 商品 1 有种子库存（WH-A-01-01）
        assertThrows(BusinessException.class, () -> productService.delete(1L));
        assertTrue(productRepository.existsById(1L), "删除被拒后商品应保留");
    }

    @Test
    @DisplayName("无关联库存：删除成功")
    void shouldDeleteWhenNoInventory() {
        // 新建一个无库存的商品再删除，避免依赖种子数据执行顺序
        Product p = productRepository.save(Product.builder()
                .name("临时商品")
                .sku("SKU-TMP-" + System.currentTimeMillis())
                .unit("个")
                .build());

        productService.delete(p.getId());
        assertFalse(productRepository.existsById(p.getId()), "无关联库存的商品应删除成功");
    }

    @Test
    @DisplayName("商品列表：支持分页且总数正确")
    void shouldListWithPagination() {
        var page = productService.list(null, 1, 5);
        assertTrue(page.getTotal() >= 5);
        assertTrue(page.getList().size() <= 5);
        assertEquals(1, page.getPage());
        assertEquals(5, page.getPageSize());
    }
}
