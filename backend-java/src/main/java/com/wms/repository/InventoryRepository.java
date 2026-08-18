package com.wms.repository;

import com.wms.dto.InventoryResponse;
import com.wms.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 库存 Repository
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndLocationCode(Long productId, String locationCode);

    boolean existsByProductId(Long productId);

    /**
     * 悲观锁查询：SELECT ... FOR UPDATE，在事务内锁定库存行，防止出库并发超卖
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId AND i.locationCode = :locationCode")
    Optional<Inventory> findForUpdate(@Param("productId") Long productId,
                                      @Param("locationCode") String locationCode);

    /**
     * 库存查询：单条 JOIN 查询一次带出商品名/SKU/仓库名，避免 N+1；支持模糊搜索、仓库筛选、分页
     */
    @Query(value = "SELECT NEW com.wms.dto.InventoryResponse(i.productId, p.name, p.sku, i.locationCode, w.name, i.quantity, i.updatedAt) " +
            "FROM Inventory i " +
            "JOIN Product p ON p.id = i.productId " +
            "JOIN Location l ON l.code = i.locationCode " +
            "JOIN Warehouse w ON w.id = l.warehouseId " +
            "WHERE (:keyword IS NULL OR p.name LIKE %:keyword% OR p.sku LIKE %:keyword%) " +
            "AND (:warehouseId IS NULL OR l.warehouseId = :warehouseId)",
            countQuery = "SELECT COUNT(i) FROM Inventory i " +
                    "JOIN Product p ON p.id = i.productId " +
                    "JOIN Location l ON l.code = i.locationCode " +
                    "JOIN Warehouse w ON w.id = l.warehouseId " +
                    "WHERE (:keyword IS NULL OR p.name LIKE %:keyword% OR p.sku LIKE %:keyword%) " +
                    "AND (:warehouseId IS NULL OR l.warehouseId = :warehouseId)")
    Page<InventoryResponse> queryInventory(@Param("keyword") String keyword,
                                           @Param("warehouseId") Long warehouseId,
                                           Pageable pageable);
}
