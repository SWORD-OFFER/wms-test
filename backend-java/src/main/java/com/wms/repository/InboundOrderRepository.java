package com.wms.repository;

import com.wms.entity.InboundOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 入库单 Repository
 */
@Repository
public interface InboundOrderRepository extends JpaRepository<InboundOrder, Long> {

    /**
     * 查询指定前缀（当天单号前缀 IN-YYYYMMDD-）下已创建的订单数，用于生成递增序号
     */
    @Query("SELECT COUNT(o) FROM InboundOrder o WHERE o.orderNo LIKE CONCAT(:prefix, '%')")
    long countByOrderNoPrefix(@Param("prefix") String prefix);
}
