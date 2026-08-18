<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getInventory, getWarehouses, type Warehouse, type InventoryItem } from '@/api'
import { isLowStock } from '@/utils/inventory'

const keyword = ref('')
const warehouseId = ref<number | undefined>()
const loading = ref(false)
const inventoryList = ref<InventoryItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const warehouses = ref<Warehouse[]>([])

const loadInventory = async () => {
  loading.value = true
  try {
    const res = await getInventory({
      keyword: keyword.value || undefined,
      warehouseId: warehouseId.value,
      page: page.value,
      pageSize: pageSize.value,
    })
    inventoryList.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

// 搜索防抖：输入停顿 300ms 才请求，避免每敲一个字符都打后端
let timer: ReturnType<typeof setTimeout> | undefined
const onKeywordInput = () => {
  clearTimeout(timer)
  timer = setTimeout(() => {
    page.value = 1
    loadInventory()
  }, 300)
}

const onSearch = () => {
  clearTimeout(timer)
  page.value = 1
  loadInventory()
}

const onWarehouseChange = () => {
  page.value = 1
  loadInventory()
}

// 低库存（<10）行高亮为红色
const getRowStyle = ({ row }: { row: InventoryItem }) => {
  if (isLowStock(row.quantity)) {
    return { color: '#f56c6c', fontWeight: 'bold' as const }
  }
  return {}
}

const onPageChange = (p: number) => {
  page.value = p
  loadInventory()
}

onMounted(async () => {
  warehouses.value = (await getWarehouses()).data
  loadInventory()
})
</script>

<template>
  <div>
    <h3>库存查询</h3>

    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称/SKU..."
        style="width: 300px"
        clearable
        @input="onKeywordInput"
        @clear="onSearch"
        @keyup.enter="onSearch"
      />
      <el-select
        v-model="warehouseId"
        placeholder="选择仓库"
        clearable
        style="width: 200px"
        @change="onWarehouseChange"
      >
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-button type="primary" @click="onSearch">查询</el-button>
    </div>

    <el-table :data="inventoryList" v-loading="loading" border stripe :row-style="getRowStyle">
      <el-table-column prop="productName" label="商品名称" />
      <el-table-column prop="sku" label="SKU" width="150" />
      <el-table-column prop="locationCode" label="库位编码" width="150" />
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="quantity" label="库存数量" width="100" />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
    </el-table>

    <div class="pager">
      <el-pagination
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="onPageChange"
      />
    </div>

    <el-empty v-if="!loading && inventoryList.length === 0" description="暂无库存数据，请先完成入库操作" />
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
  text-align: right;
}
</style>
