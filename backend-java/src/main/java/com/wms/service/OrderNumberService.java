package com.wms.service;

import com.wms.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * 订单单号生成相关的并发重试支持。
 * count+1 生成单号在真并发下会撞唯一约束，这里在独立事务中执行，
 * 撞约束时自动重试取下一个序号，避免请求直接失败。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNumberService {

    private final TransactionTemplate transactionTemplate;

    private static final int MAX_ORDER_NO_RETRY = 20;

    /**
     * 每次尝试在独立事务中执行，撞唯一约束则重试
     */
    public <T> T withOrderNoRetry(String action, Supplier<T> supplier) {
        for (int attempt = 1; ; attempt++) {
            try {
                return transactionTemplate.execute(status -> supplier.get());
            } catch (DataIntegrityViolationException e) {
                if (attempt >= MAX_ORDER_NO_RETRY) {
                    log.error("{}, 重试 {} 次后仍单号冲突", action, attempt);
                    throw new BusinessException(action + "失败：单号冲突，请重试");
                }
                log.warn("{}, 单号冲突，重试第 {} 次", action, attempt + 1);
            }
        }
    }
}
