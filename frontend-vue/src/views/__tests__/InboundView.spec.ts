import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ElementPlus, { ElMessage } from 'element-plus'
import InboundView from '../InboundView.vue'
import type { Warehouse, Location, Product } from '@/api'

// Mock API 模块，避免真实 HTTP 请求
const api = vi.hoisted(() => ({
  createInboundOrder: vi.fn(),
  getProducts: vi.fn(),
  getWarehouses: vi.fn(),
  getLocations: vi.fn(),
}))
vi.mock('@/api', () => api)

const products: Product[] = [
  { id: 1, name: '蓝牙耳机 Pro', sku: 'SKU-001', unit: '个', createdAt: '', updatedAt: '' },
]
const warehouses: Warehouse[] = [{ id: 1, code: 'WH-A', name: '广州主仓' }]
const locations: Location[] = [
  { id: 1, warehouseId: 1, code: 'WH-A-01-01', status: 'FREE' },
  { id: 2, warehouseId: 1, code: 'WH-A-01-02', status: 'FREE' },
]

const mountView = () => mount(InboundView, { global: { plugins: [ElementPlus] } })

/** script setup 内部绑定不在公开实例类型上，测试中按需取值 */
const vm = (wrapper: ReturnType<typeof mountView>) => wrapper.vm as any

describe('InboundView 入库单表单', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.getProducts.mockResolvedValue({ data: products })
    api.getWarehouses.mockResolvedValue({ data: warehouses })
    api.getLocations.mockResolvedValue({ data: locations })
  })

  it('渲染供应商输入框、一行明细和提交按钮', async () => {
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('input[placeholder="请输入供应商名称"]').exists()).toBe(true)
    expect(wrapper.findAll('.row').length).toBe(1)
    expect(wrapper.text()).toContain('提交入库单')
  })

  it('点击添加/删除按钮可增删明细行', async () => {
    const wrapper = mountView()
    await flushPromises()

    const addBtn = wrapper.findAll('button').find((b) => b.text().includes('添加明细'))!
    await addBtn.trigger('click')
    expect(wrapper.findAll('.row').length).toBe(2)

    // 删除一行后回到一行
    const delBtn = wrapper.findAll('button').find((b) => b.text().includes('删除'))!
    await delBtn.trigger('click')
    expect(wrapper.findAll('.row').length).toBe(1)
  })

  it('空表单提交：弹警告且不调用创建接口', async () => {
    const warningSpy = vi.spyOn(ElMessage, 'warning')
    const wrapper = mountView()
    await flushPromises()

    const submitBtn = wrapper.findAll('button').find((b) => b.text().includes('提交入库单'))!
    await submitBtn.trigger('click')

    expect(warningSpy).toHaveBeenCalled()
    expect(api.createInboundOrder).not.toHaveBeenCalled()
    warningSpy.mockRestore()
  })

  it('合法提交：调用 createInboundOrder 且 payload 正确', async () => {
    api.createInboundOrder.mockResolvedValue({ data: { orderNo: 'IN-20260819-001' } })
    const successSpy = vi.spyOn(ElMessage, 'success')
    const wrapper = mountView()
    await flushPromises()

    await wrapper.find('input[placeholder="请输入供应商名称"]').setValue('SupplierA')
    vm(wrapper).items = [{ id: 1, productId: 1, warehouseId: 1, locationCode: 'WH-A-01-01', quantity: 5, locations: [] }]

    const submitBtn = wrapper.findAll('button').find((b) => b.text().includes('提交入库单'))!
    await submitBtn.trigger('click')
    await flushPromises()

    expect(api.createInboundOrder).toHaveBeenCalledWith({
      supplierName: 'SupplierA',
      items: [{ productId: 1, quantity: 5, locationCode: 'WH-A-01-01' }],
    })
    expect(successSpy).toHaveBeenCalled()
    successSpy.mockRestore()
  })

  it('选择仓库：加载该仓库库位并重置库位编码', async () => {
    const wrapper = mountView()
    await flushPromises()

    const row = { productId: undefined, warehouseId: 1, locationCode: 'OLD', quantity: 1, locations: [] }
    await vm(wrapper).onWarehouseChange(row)

    expect(api.getLocations).toHaveBeenCalledWith(1)
    expect(row.locationCode).toBe('')
    expect(row.locations).toHaveLength(2)
  })
})
