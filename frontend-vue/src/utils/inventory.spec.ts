import { describe, expect, it } from 'vitest'
import { isLowStock, LOW_STOCK_THRESHOLD } from './inventory'

describe('isLowStock 低库存判定', () => {
  it('低于阈值(10)判定为低库存', () => {
    expect(isLowStock(0)).toBe(true)
    expect(isLowStock(9)).toBe(true)
  })

  it('达到阈值(10)及以上判定为正常', () => {
    expect(isLowStock(10)).toBe(false)
    expect(isLowStock(100)).toBe(false)
  })

  it('边界值与阈值常量一致', () => {
    expect(isLowStock(LOW_STOCK_THRESHOLD)).toBe(false)
    expect(isLowStock(LOW_STOCK_THRESHOLD - 1)).toBe(true)
  })
})
