import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import InventoryView from '../InventoryView.vue'
import type { InventoryItem, Warehouse } from '@/api'

const api = vi.hoisted(() => ({
  getInventory: vi.fn(),
  getWarehouses: vi.fn(),
}))
vi.mock('@/api', () => api)

const warehouses: Warehouse[] = [{ id: 1, code: 'WH-A', name: '广州主仓' }]

const lowStock: InventoryItem = {
  productId: 3, productName: '无线充电板', sku: 'SKU-003', locationCode: 'WH-A-01-02',
  warehouseName: '广州主仓', quantity: 5, updatedAt: '2026-08-19T00:00:00',
}
const normalStock: InventoryItem = {
  productId: 1, productName: '蓝牙耳机 Pro', sku: 'SKU-001', locationCode: 'WH-A-01-01',
  warehouseName: '广州主仓', quantity: 150, updatedAt: '2026-08-19T00:00:00',
}

const mountView = () => mount(InventoryView, { global: { plugins: [ElementPlus] } })

/** script setup 内部绑定不在公开实例类型上，测试中按需取值 */
const vm = (wrapper: ReturnType<typeof mountView>) => wrapper.vm as any

describe('InventoryView 库存列表', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.getWarehouses.mockResolvedValue({ data: warehouses })
    api.getInventory.mockResolvedValue({
      data: { list: [lowStock, normalStock], total: 2, page: 1, pageSize: 20 },
    })
  })

  it('挂载后加载库存并渲染列表', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(api.getInventory).toHaveBeenCalled()
    await flushPromises()
    expect(wrapper.text()).toContain('无线充电板')
    expect(wrapper.text()).toContain('蓝牙耳机 Pro')
  })

  it('低库存(<10)行样式命中，正常库存不命中', async () => {
    const wrapper = mountView()
    await flushPromises()

    const styleLow = vm(wrapper).getRowStyle({ row: lowStock })
    expect(styleLow.color).toBe('#f56c6c')

    const styleNormal = vm(wrapper).getRowStyle({ row: normalStock })
    expect(styleNormal).toEqual({})
  })

  it('切换页码触发重新加载', async () => {
    const wrapper = mountView()
    await flushPromises()
    const before = api.getInventory.mock.calls.length

    await vm(wrapper).onPageChange(2)
    await flushPromises()

    expect(api.getInventory.mock.calls.length).toBe(before + 1)
    expect(api.getInventory.mock.calls[before][0].page).toBe(2)
  })
})
