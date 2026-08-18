package com.wms.repository;

import com.wms.entity.OutboundOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 出库单明细 Repository
 */
@Repository
public interface OutboundOrderItemRepository extends JpaRepository<OutboundOrderItem, Long> {
}
