<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createInboundOrder,
  getProducts,
  getWarehouses,
  getLocations,
  type Product,
  type Warehouse,
  type Location,
} from '@/api'

interface InboundRow {
  productId?: number
  warehouseId?: number
  locationCode: string
  quantity: number
  locations: Location[]
}

const supplierName = ref('')
const submitting = ref(false)
const products = ref<Product[]>([])
const warehouses = ref<Warehouse[]>([])
const items = ref<InboundRow[]>([])

const newRow = (): InboundRow => ({
  productId: undefined,
  warehouseId: undefined,
  locationCode: '',
  quantity: 1,
  locations: [],
})

const addItem = () => {
  items.value.push(newRow())
}

const removeItem = (index: number) => {
  items.value.splice(index, 1)
}

// 商品远程搜索（防抖，避免每敲一个字符都请求）
let productTimer: ReturnType<typeof setTimeout> | undefined
const searchProducts = (query: string) => {
  clearTimeout(productTimer)
  productTimer = setTimeout(async () => {
    const res = await getProducts(query || undefined)
    products.value = res.data
  }, 200)
}

// 仓库 → 库位级联：选仓库后拉取该仓库库位，切换时重置库位
const onWarehouseChange = async (row: InboundRow) => {
  row.locationCode = ''
  row.locations = []
  if (!row.warehouseId) return
  const res = await getLocations(row.warehouseId)
  row.locations = res.data
}

const handleSubmit = async () => {
  if (!supplierName.value.trim()) {
    ElMessage.warning('请输入供应商名称')
    return
  }
  if (items.value.length === 0) {
    ElMessage.warning('请添加至少一行入库明细')
    return
  }
  const invalid = items.value.some(
    (it) => !it.productId || !it.locationCode || !it.quantity || it.quantity < 1
  )
  if (invalid) {
    ElMessage.warning('请完整填写每行明细：商品、库位、数量(≥1)')
    return
  }
  submitting.value = true
  try {
    const res = await createInboundOrder({
      supplierName: supplierName.value.trim(),
      items: items.value.map((it) => ({
        productId: it.productId!,
        quantity: it.quantity,
        locationCode: it.locationCode,
      })),
    })
    ElMessage.success('入库单创建成功：' + res.data.orderNo)
    supplierName.value = ''
    items.value = [newRow()]
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  warehouses.value = (await getWarehouses()).data
  products.value = (await getProducts()).data
  items.value = [newRow()]
})
</script>

<template>
  <div>
    <h3>入库管理</h3>

    <el-form label-width="100px" style="max-width: 900px">
      <el-form-item label="供应商名称" required>
        <el-input v-model="supplierName" placeholder="请输入供应商名称" maxlength="200" clearable />
      </el-form-item>

      <el-form-item label="入库明细">
        <el-button type="primary" @click="addItem">+ 添加明细</el-button>
      </el-form-item>

      <div v-for="(item, index) in items" :key="index" class="row">
        <el-select
          v-model="item.productId"
          placeholder="选择商品"
          filterable
          remote
          :remote-method="searchProducts"
          style="width: 200px"
        >
          <el-option v-for="p in products" :key="p.id" :label="p.name + ' / ' + p.sku" :value="p.id" />
        </el-select>

        <el-select
          v-model="item.warehouseId"
          placeholder="选择仓库"
          style="width: 140px"
          @change="onWarehouseChange(item)"
        >
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>

        <el-select
          v-model="item.locationCode"
          placeholder="选择库位"
          style="width: 170px"
          :disabled="!item.warehouseId"
        >
          <el-option v-for="l in item.locations" :key="l.code" :label="l.code" :value="l.code" />
        </el-select>

        <el-input-number v-model="item.quantity" :min="1" :max="999999" style="width: 130px" />

        <el-button type="danger" size="small" @click="removeItem(index)" :disabled="items.length === 1">
          删除
        </el-button>
      </div>
    </el-form>

    <el-button type="success" :loading="submitting" :disabled="items.length === 0" @click="handleSubmit">
      提交入库单
    </el-button>

    <el-empty v-if="items.length === 0" description="请点击“添加明细”按钮添加入库商品" />
  </div>
</template>

<style scoped>
.row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}
</style>
