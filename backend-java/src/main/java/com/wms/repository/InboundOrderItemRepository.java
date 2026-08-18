package com.wms.repository;

import com.wms.entity.InboundOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 入库单明细 Repository
 */
@Repository
public interface InboundOrderItemRepository extends JpaRepository<InboundOrderItem, Long> {
}
