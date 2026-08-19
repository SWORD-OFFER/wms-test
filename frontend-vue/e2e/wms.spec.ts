import { test, expect, type Page } from '@playwright/test'

/**
 * WMS 端到端测试
 * 前置：后端 http://localhost:8080 与前端 http://localhost:5173 均需已启动（H2 种子数据干净）
 */

/** 在 Element Plus el-select 下拉中选择一项（popper 传送至 body，按文本匹配） */
async function pickSelect(page: Page, placeholder: string, optionText: string) {
  await page.locator('.el-select').filter({ hasText: placeholder }).click()
  await page.locator('.el-select-dropdown__item', { hasText: optionText }).first().click()
}

test.describe('WMS 全流程', () => {
  test('商品管理：列表加载 + 搜索', async ({ page }) => {
    await page.goto('/#/products')
    await expect(page.getByText('蓝牙耳机 Pro')).toBeVisible()
    await expect(page.getByText('屏幕保护膜')).toBeVisible()

    const search = page.getByPlaceholder('搜索商品名称/SKU...')
    await search.fill('蓝牙')
    await search.press('Enter')
    await expect(page.getByText('蓝牙耳机 Pro')).toBeVisible()
    await expect(page.getByText('Type-C 数据线')).toBeHidden()
  })

  test('入库：创建入库单成功', async ({ page }) => {
    await page.goto('/#/inbound')
    await page.getByPlaceholder('请输入供应商名称').fill('E2E供应商')

    // 选商品（远程下拉，选项为 名称 / SKU）
    await pickSelect(page, '选择商品', '蓝牙耳机 Pro')
    // 选仓库 → 触发级联加载库位
    await pickSelect(page, '选择仓库', '广州主仓')
    await page.waitForTimeout(300)
    // 选库位
    await pickSelect(page, '选择库位', 'WH-A-01-01')
    // 数量输入
    await page.locator('.el-input-number input').first().fill('3')

    await page.getByRole('button', { name: '提交入库单' }).click()
    await expect(page.getByText('入库单创建成功')).toBeVisible()
  })

  test('库存：列表加载 + 低库存高亮', async ({ page }) => {
    await page.goto('/#/inventory')
    // 蓝牙耳机 Pro 有两个库位记录，用 first 规避 strict mode
    await expect(page.getByText('蓝牙耳机 Pro').first()).toBeVisible()
    await expect(page.getByText('无线充电板')).toBeVisible()

    // 低库存(<10)行应为红色加粗（无线充电板=5）
    const lowRow = page.locator('.el-table__row', { hasText: '无线充电板' })
    await expect(lowRow).toHaveCSS('color', 'rgb(245, 108, 108)')
  })

  test('出库：创建出库单成功', async ({ page }) => {
    await page.goto('/#/outbound')
    await page.getByPlaceholder('请输入客户名称').fill('E2E客户')

    await pickSelect(page, '选择商品', '蓝牙耳机 Pro')
    await pickSelect(page, '选择仓库', '广州主仓')
    await page.waitForTimeout(300)
    await pickSelect(page, '选择库位', 'WH-A-01-01')
    await page.locator('.el-input-number input').first().fill('2')

    await page.getByRole('button', { name: '提交出库单' }).click()
    await expect(page.getByText('出库单创建成功')).toBeVisible()
  })

  test('Bug1：删除有关联库存的商品被拒绝', async ({ page }) => {
    await page.goto('/#/products')
    const row = page.locator('.el-table__row', { hasText: '蓝牙耳机 Pro' })
    await row.getByRole('button', { name: '删除' }).click()
    // 确认弹窗（Element Plus 默认英文 locale，确认按钮为 primary 按钮）
    await page.locator('.el-message-box .el-button--primary').click()
    await expect(page.getByText('该商品存在关联库存，无法删除')).toBeVisible()
  })
})
