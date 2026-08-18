package com.wms.repository;

import com.wms.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 库存 Repository — 候选人需要实现库存查询（任务2）
 * 提示：你可能需要添加自定义查询方法
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndLocationCode(Long productId, String locationCode);

    boolean existsByProductId(Long productId);

    // 库存查询的 JOIN 分页查询见服务层实现（InventoryService.queryInventory）
}
