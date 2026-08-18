/**
 * 库存业务纯函数：方便单元测试，不依赖 UI 组件
 */
export const LOW_STOCK_THRESHOLD = 10

/** 是否低库存（数量低于阈值） */
export const isLowStock = (quantity: number): boolean => quantity < LOW_STOCK_THRESHOLD
