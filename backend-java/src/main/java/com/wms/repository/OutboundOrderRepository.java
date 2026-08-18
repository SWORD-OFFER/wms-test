package com.wms.repository;

import com.wms.entity.OutboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 出库单 Repository
 */
@Repository
public interface OutboundOrderRepository extends JpaRepository<OutboundOrder, Long> {

    /**
     * 查询指定前缀（当天单号前缀 OUT-YYYYMMDD-）下已创建的订单数，用于生成递增序号
     */
    @Query("SELECT COUNT(o) FROM OutboundOrder o WHERE o.orderNo LIKE CONCAT(:prefix, '%')")
    long countByOrderNoPrefix(@Param("prefix") String prefix);
}
