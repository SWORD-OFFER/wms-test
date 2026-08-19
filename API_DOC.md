# WMS 接口文档

> 本文件是**实际实现**的接口文档(比 `docs/API_SPEC.md` 更完整:含实际 HTTP 状态码、校验规则、错误码、出库接口)。
> Base URL:`http://localhost:8080/api`(后端 Spring Boot 8080;前端经 `/api` 代理访问)
> 交互式文档:启动后端后访问 `http://localhost:8080/swagger-ui.html`

---

## 通用约定

### 响应格式

```json
{ "code": 200, "message": "success", "data": { } }
```

- 成功:`code = 200`;创建成功:`code = 201`(HTTP 状态同步为 201)
- 业务错误:`code = 400`(或 404),`data = null`
- 参数校验失败:`code = 400`,message 拼接字段错误
- 系统异常:`code = 500`,message `服务器内部错误`

### 分页响应

```json
{
  "code": 200, "message": "success",
  "data": { "list": [], "total": 100, "page": 1, "pageSize": 20 }
}
```

### 错误码表

| HTTP | code | 场景 |
|------|------|------|
| 200 | 200 | 查询/更新/删除成功 |
| 201 | 201 | 创建成功(入库单/出库单) |
| 400 | 400 | 业务校验失败(库位不存在、库存不足、SKU 已存在等) |
| 400 | 400 | 参数校验失败(字段为空、数量不合法等) |
| 404 | 404 | 资源不存在(商品不存在) |
| 500 | 500 | 未捕获异常 |

---

## 1. 商品管理 `/api/products`

### 1.1 商品列表
`GET /api/products?keyword=`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 否 | 商品名称/SKU 模糊搜索 |

```json
// 响应 data
[
  { "id": 1, "name": "蓝牙耳机 Pro", "sku": "SKU-001", "unit": "个",
    "createdAt": "2026-08-19T02:21:56.755039", "updatedAt": "2026-08-19T02:21:56.755039" }
]
```

### 1.2 商品详情
`GET /api/products/{id}`

- 商品不存在 → `404 "商品不存在"`

### 1.3 新增商品
`POST /api/products`

```json
// 请求体
{ "name": "蓝牙耳机 Pro", "sku": "SKU-001", "unit": "个" }
```

- `name`/`sku` 必填;SKU 已存在 → `400 "SKU已存在: SKU-001"`

### 1.4 更新商品
`PUT /api/products/{id}`

```json
{ "name": "新名称", "unit": "个" }
```

### 1.5 删除商品
`DELETE /api/products/{id}`

- 有关联库存 → `400 "该商品存在关联库存，无法删除"`
- 商品不存在 → `404 "商品不存在"`
- 成功 → `200 "删除成功"`

---

## 2. 仓库 & 库位 `/api`

### 2.1 仓库列表
`GET /api/warehouses`

```json
// 响应 data
[ { "id": 1, "code": "WH-A", "name": "广州主仓" },
  { "id": 2, "code": "WH-B", "name": "深圳保税仓" } ]
```

### 2.2 某仓库下的库位列表
`GET /api/warehouses/{id}/locations`

```json
// 响应 data
[ { "id": 1, "warehouseId": 1, "code": "WH-A-01-01", "status": "OCCUPIED" } ]
```

---

## 3. 入库单 `/api/inbound-orders`

### 3.1 创建入库单
`POST /api/inbound-orders` → **HTTP 201**

```json
// 请求体
{
  "supplierName": "供应商A",
  "items": [
    { "productId": 1, "quantity": 100, "locationCode": "WH-A-01-01" }
  ]
}
```

```json
// 响应 data
{
  "id": 1, "orderNo": "IN-20260819-001", "supplierName": "供应商A", "status": "COMPLETED",
  "items": [ { "productId": 1, "productName": "蓝牙耳机 Pro", "quantity": 100, "locationCode": "WH-A-01-01" } ],
  "createdAt": "2026-08-19T10:00:00"
}
```

**业务规则**:创建时在**事务**内自动累加对应库位库存;单号 `IN-YYYYMMDD-XXX` 自动生成(并发冲突自动重试)。

**校验规则**:
| 字段 | 规则 |
|------|------|
| supplierName | `@NotBlank` |
| items | `@NotEmpty` + 嵌套 `@Valid` |
| items[].productId | `@NotNull` |
| items[].quantity | `@Min(1)` + `@Max(999999)` |
| items[].locationCode | `@NotBlank` |

**错误**:商品不存在 `404`;库位不存在 `400 "库位不存在: XXX"`。

---

## 4. 库存查询 `/api/inventory`

### 4.1 查询库存
`GET /api/inventory?keyword=&warehouseId=&page=&pageSize=`

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| keyword | string | 否 | - | 商品名称/SKU 模糊搜索 |
| warehouseId | long | 否 | - | 仓库 ID 筛选 |
| page | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 20 | 每页条数 |

```json
// 响应 data
{
  "list": [
    { "productId": 1, "productName": "蓝牙耳机 Pro", "sku": "SKU-001",
      "locationCode": "WH-A-01-01", "warehouseName": "广州主仓", "quantity": 180,
      "updatedAt": "2026-08-19T09:30:00" }
  ],
  "total": 50, "page": 1, "pageSize": 20
}
```

> 实现说明:单条 JPQL JOIN 一次带出商品名/仓库名,避免 N+1。

---

## 5. 出库单 `/api/outbound-orders`(选做 A)

### 5.1 创建出库单
`POST /api/outbound-orders` → **HTTP 201**

```json
// 请求体
{
  "customerName": "客户X",
  "items": [ { "productId": 1, "quantity": 10, "locationCode": "WH-A-01-01" } ]
}
```

```json
// 响应 data
{
  "id": 1, "orderNo": "OUT-20260819-001", "customerName": "客户X", "status": "COMPLETED",
  "items": [ { "productId": 1, "productName": "蓝牙耳机 Pro", "quantity": 10, "locationCode": "WH-A-01-01" } ],
  "createdAt": "2026-08-19T10:00:00"
}
```

**并发安全**:悲观锁 `SELECT ... FOR UPDATE` 锁定库存行,锁内校验余额再扣减,防止超卖。

**错误**:
- 库存不存在 → `400 "库存不存在: product=X, location=XXX"`
- 库存不足 → `400 "库存不足: 商品 X 当前库存 N，需扣减 M"`
- 商品不存在 → `404`
- 校验规则同入库单(customerName `@NotBlank` 等)

---

## 附:数据模型摘要

| 表 | 关键字段 | 约束 |
|----|---------|------|
| products | id, name, sku, unit, created_at, updated_at | sku 唯一 |
| warehouses | id, code, name | code 唯一 |
| locations | id, warehouse_id, code, status | code 唯一 |
| inventory | id, product_id, location_code, quantity, updated_at | (product_id, location_code) 唯一 |
| inbound_orders | id, order_no, supplier_name, status, created_at | order_no 唯一 |
| inbound_order_items | id, order_id, product_id, quantity, location_code | - |
| outbound_orders | id, order_no, customer_name, status, created_at | order_no 唯一 |
| outbound_order_items | id, order_id, product_id, quantity, location_code | - |
