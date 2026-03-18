# iDempiere 台灣示範資料插件設計規格

**日期**：2026-03-18
**狀態**：Draft
**作者**：Claude Code

---

## 1. 專案概述

### 1.1 背景

iDempiere 內建的 Garden World 示範資料是英文的，對台灣使用者不夠友善。本專案旨在建立一個繁體中文的示範環境，包含貼近台灣商業情境的基礎資料，讓台灣使用者更容易學習和展示 iDempiere 的功能。

### 1.2 目標

- 提供繁體中文的示範 Client（Tenant）
- 包含台灣會計科目、商品、客戶/供應商等基礎主檔
- 以 OSGi 插件形式發布，安裝即可使用
- 支援乾淨的重置機制

### 1.3 專案名稱

`tw.idempiere.sample`

---

## 2. 前提條件

目標環境須已安裝：

1. **iDempiere 12**（或相容版本）

---

## 3. OSGi 生命週期行為

| 事件 | 行為 | 說明 |
|------|------|------|
| `start` | 初始化 | 檢查 Sample Client 是否存在；不存在則建立，已存在則跳過 |
| `stop` | 無動作 | 保留所有資料 |
| `uninstall` | 清理 | 刪除 Sample Client 及其所有資料 |

**重置流程**：卸載插件（觸發清理）→ 重新安裝（觸發初始化）

---

## 4. Tenant 設定

### 4.1 基本資訊

| 項目 | 值 |
|------|-----|
| Client 名稱 | 天地人實業有限公司 |
| Client Search Key | sample |

### 4.2 會計設定

| 項目 | 值 |
|------|-----|
| 會計架構 | 台灣商業會計處理準則（精簡版） |
| 幣別 | TWD 新台幣 |
| Costing Method | Average PO |
| 成本層級 | Client |

### 4.3 帳期設定

| 項目 | 值 |
|------|-----|
| Calendar | 天地人會計年度 |
| 帳期數量 | 12 個月 |
| 起始年度 | 當年度 |
| Automatic Period Control | 關閉 |

### 4.4 稅務設定

| 項目 | 值 |
|------|-----|
| 稅率名稱 | 營業稅 |
| 稅率 | 5% |

---

## 5. 組織架構

```
天地人實業有限公司 (Client)
├── * （共用組織，AD_Org_ID 使用系統分配）
├── 台北總公司
│   ├── 類型：總部 + 營業據點
│   └── 倉庫：台北主倉
├── 台中分公司
│   ├── 類型：營業據點
│   └── 倉庫：台中倉
└── 高雄倉庫
    ├── 類型：純倉庫
    └── 倉庫：高雄倉
```

每個倉庫預設建立一個 Locator（庫位）。

---

## 6. 會計科目

採用台灣商業會計處理準則的架構，精簡至 Demo 所需的科目：

### 6.1 科目架構

```
1 資產
├── 11 流動資產
│   ├── 1101 現金
│   ├── 1102 銀行存款
│   ├── 1103 應收帳款
│   ├── 1104 應收票據
│   ├── 1105 存貨
│   └── 1106 預付款項
├── 12 非流動資產
│   ├── 1201 固定資產
│   └── 1202 累計折舊
│
2 負債
├── 21 流動負債
│   ├── 2101 應付帳款
│   ├── 2102 應付票據
│   ├── 2103 應付薪資
│   └── 2104 應付稅款
├── 22 非流動負債
│   └── 2201 長期借款
│
3 權益
├── 31 股本
│   └── 3101 普通股股本
├── 32 保留盈餘
│   ├── 3201 法定盈餘公積
│   └── 3202 未分配盈餘
│
4 收入
├── 41 營業收入
│   ├── 4101 銷貨收入
│   └── 4102 銷貨退回
├── 42 營業外收入
│   └── 4201 利息收入
│
5 支出
├── 51 營業成本
│   └── 5101 銷貨成本
├── 52 營業費用
│   ├── 5201 薪資費用
│   ├── 5202 租金費用
│   ├── 5203 水電費
│   └── 5204 運費
├── 53 營業外支出
│   └── 5301 利息費用
```

### 6.2 預設科目對應

系統需要的預設科目將對應至上述科目，例如：
- Product Revenue → 4101 銷貨收入
- Product COGS → 5101 銷貨成本
- Product Asset → 1105 存貨
- Customer Receivable → 1103 應收帳款
- Vendor Liability → 2101 應付帳款

---

## 7. 主檔資料

### 7.1 Business Partner（15 筆）

#### 供應商（5 筆）

| 名稱 | Search Key | 說明 |
|------|------------|------|
| 大同文具股份有限公司 | TATUNG-STATIONERY | 主要文具供應商 |
| 金車物流有限公司 | KINGCAR-LOGISTICS | 物流服務供應商 |
| 供應商 A | VENDOR-A | 一般供應商 |
| 供應商 B | VENDOR-B | 一般供應商 |
| 供應商 C | VENDOR-C | 一般供應商 |

#### 客戶（7 筆）

| 名稱 | Search Key | 說明 |
|------|------------|------|
| 誠品書店 | ESLITE | 大型零售客戶 |
| 全家便利商店 | FAMILY-MART | 連鎖通路客戶 |
| 台北市政府 | TAIPEI-GOV | 政府機關客戶 |
| 客戶 A | CUSTOMER-A | 一般客戶 |
| 客戶 B | CUSTOMER-B | 一般客戶 |
| 客戶 C | CUSTOMER-C | 一般客戶 |
| 客戶 D | CUSTOMER-D | 一般客戶 |

#### 員工（3 筆）

| 名稱 | Search Key | 說明 |
|------|------------|------|
| 王小明 | EMP-WANG | 業務人員 |
| 李小華 | EMP-LEE | 倉管人員 |
| 員工 A | EMP-A | 一般員工 |

### 7.2 Product（30 筆）

#### 庫存品 - Item（20 筆）

| 名稱 | Search Key | 類別 | 單位 |
|------|------------|------|------|
| A4 影印紙（500張） | PAPER-A4-500 | 紙類 | 包 |
| A4 影印紙（箱裝） | PAPER-A4-BOX | 紙類 | 箱 |
| 原子筆-藍 | PEN-BLUE | 書寫工具 | 支 |
| 原子筆-黑 | PEN-BLACK | 書寫工具 | 支 |
| 原子筆-紅 | PEN-RED | 書寫工具 | 支 |
| 鉛筆 HB | PENCIL-HB | 書寫工具 | 支 |
| 橡皮擦 | ERASER | 書寫工具 | 個 |
| 筆記本-25K | NOTEBOOK-25K | 紙類 | 本 |
| 筆記本-18K | NOTEBOOK-18K | 紙類 | 本 |
| 資料夾-A4 | FOLDER-A4 | 檔案用品 | 個 |
| 迴紋針（盒裝） | CLIP-BOX | 辦公用品 | 盒 |
| 釘書機 | STAPLER | 辦公用品 | 台 |
| 釘書針（盒裝） | STAPLE-BOX | 辦公用品 | 盒 |
| 剪刀 | SCISSORS | 辦公用品 | 把 |
| 膠帶台 | TAPE-DISPENSER | 辦公用品 | 台 |
| 透明膠帶 | TAPE-CLEAR | 辦公用品 | 捲 |
| 商品 A | ITEM-A | 一般商品 | 個 |
| 商品 B | ITEM-B | 一般商品 | 個 |
| 商品 C | ITEM-C | 一般商品 | 個 |
| 商品 D | ITEM-D | 一般商品 | 個 |

#### 服務 - Service（5 筆）

| 名稱 | Search Key | 類別 |
|------|------------|------|
| 國內運費 | SVC-SHIPPING-TW | 服務 |
| 安裝服務 | SVC-INSTALL | 服務 |
| 維修服務 | SVC-REPAIR | 服務 |
| 服務 A | SVC-A | 服務 |
| 服務 B | SVC-B | 服務 |

#### 組合品 - BOM（5 筆）

| 名稱 | Search Key | 組成 |
|------|------------|------|
| 文具禮盒組 | BOM-GIFT-SET | 原子筆-藍 x1 + 筆記本-25K x1 + 資料夾-A4 x1 |
| 辦公文具組 | BOM-OFFICE-SET | 釘書機 x1 + 剪刀 x1 + 膠帶台 x1 |
| 學生文具組 | BOM-STUDENT-SET | 鉛筆 HB x3 + 橡皮擦 x1 + 筆記本-25K x2 |
| 組合品 A | BOM-A | 商品 A x2 + 商品 B x1 |
| 組合品 B | BOM-B | 商品 C x1 + 商品 D x3 |

### 7.3 其他主檔

#### Product Category

| 名稱 | 說明 |
|------|------|
| 紙類 | 影印紙、筆記本等 |
| 書寫工具 | 原子筆、鉛筆等 |
| 辦公用品 | 釘書機、剪刀等 |
| 檔案用品 | 資料夾等 |
| 服務 | 運費、安裝等 |
| 一般商品 | 其他商品 |

#### BP Group

| 名稱 | 說明 |
|------|------|
| 供應商 | Vendor 群組 |
| 客戶 | Customer 群組 |
| 員工 | Employee 群組 |

#### Payment Term

| 名稱 | 條件 |
|------|------|
| 現金 | 立即付款 |
| 月結 30 天 | 30 天後付款 |
| 月結 60 天 | 60 天後付款 |

#### Price List

| 名稱 | 幣別 | 用途 |
|------|------|------|
| 標準售價 | TWD | 銷售用 |
| 標準進價 | TWD | 採購用 |

---

## 8. 技術架構

### 8.1 專案結構

```
tw.idempiere.sample/
├── src/
│   └── tw/idempiere/sample/
│       ├── Activator.java                  # OSGi 生命週期
│       ├── setup/
│       │   ├── SampleClientSetup.java      # Client 初始化（類似 MSetup）
│       │   ├── SampleOrgSetup.java         # 組織/倉庫設定
│       │   ├── SampleAccountingSetup.java  # 會計架構設定
│       │   └── SampleMasterDataSetup.java  # 主檔資料設定
│       ├── cleanup/
│       │   └── SampleClientCleanup.java    # 清理服務
│       └── data/
│           ├── ChartOfAccountsTW.java      # 台灣會計科目定義
│           ├── OrganizationData.java       # 組織定義
│           ├── ProductData.java            # 商品定義
│           ├── BPartnerData.java           # BP 定義
│           └── MasterData.java             # 其他主檔定義
├── META-INF/
│   └── MANIFEST.MF
├── OSGI-INF/
│   └── component.xml
├── pom.xml
└── README.md
```

### 8.2 核心類別說明

#### Activator.java

```java
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) {
        // 檢查 Sample Client 是否存在
        MClient client = MClient.get(Env.getCtx(), "sample");
        if (client == null) {
            SampleClientSetup.init();
        }
    }

    @Override
    public void stop(BundleContext context) {
        // 不做任何事，保留資料
    }
}
```

需要註冊 uninstall hook 來處理清理：
```java
context.addBundleListener(event -> {
    if (event.getType() == BundleEvent.UNINSTALLED) {
        SampleClientCleanup.cleanup();
    }
});
```

#### SampleClientSetup.java

使用 Model API 建立 Client，參考 `org.compiere.model.MSetup`：

```java
public class SampleClientSetup {

    public static void init() {
        // 1. 建立 Client
        MClient client = new MClient(Env.getCtx(), 0, null);
        client.setValue("sample");
        client.setName("Sample");
        client.saveEx();

        // 2. 建立 Organization
        SampleOrgSetup.createOrganizations(client);

        // 3. 設定會計架構
        SampleAccountingSetup.setup(client);

        // 4. 建立主檔資料
        SampleMasterDataSetup.createAll(client);
    }
}
```

#### SampleAccountingSetup.java

使用 Model API 建立會計架構：

```java
public class SampleAccountingSetup {

    public static void setup(MClient client) {
        // 1. 建立 Calendar 和 Period
        MCalendar calendar = new MCalendar(client);
        calendar.setName("Sample 會計年度");
        calendar.saveEx();
        createPeriods(calendar);  // 建立 12 個月帳期

        // 2. 建立 Element（科目表）
        MElement element = new MElement(client, "台灣會計科目");
        element.saveEx();

        // 3. 建立 ElementValue（各科目）
        ChartOfAccountsTW.createAccounts(element);

        // 4. 建立 Accounting Schema
        MAcctSchema as = new MAcctSchema(client, "Sample 會計架構");
        as.setC_Currency_ID(TWD_CURRENCY_ID);
        as.setCostingMethod(MAcctSchema.COSTINGMETHOD_AveragePO);
        as.setAutoPeriodControl(false);
        as.saveEx();

        // 5. 設定 Default Accounts
        setupDefaultAccounts(as);
    }
}
```

#### SampleMasterDataSetup.java

使用 Model API 建立主檔：

```java
public class SampleMasterDataSetup {

    public static void createAll(MClient client) {
        // 1. Product Category
        createProductCategories(client);

        // 2. BP Group
        createBPGroups(client);

        // 3. Tax
        createTax(client);

        // 4. Payment Term
        createPaymentTerms(client);

        // 5. Price List
        MPriceList salesPL = createPriceList(client, "標準售價", true);
        MPriceList purchasePL = createPriceList(client, "標準進價", false);

        // 6. Business Partners
        BPartnerData.createAll(client);

        // 7. Products
        ProductData.createAll(client, salesPL, purchasePL);

        // 8. BOM
        ProductData.createBOMs(client);
    }
}
```

#### SampleClientCleanup.java

清理資料（參考 Delete Client 插件）：

```java
public class SampleClientCleanup {

    public static void cleanup() {
        MClient client = MClient.get(Env.getCtx(), "sample");
        if (client == null) return;

        int clientId = client.getAD_Client_ID();

        // 1. 刪除交易資料（若有）
        deleteTransactions(clientId);

        // 2. 刪除 BOM
        deleteBOMs(clientId);

        // 3. 刪除 Product Price
        deleteProductPrices(clientId);

        // 4. 刪除 Product
        deleteProducts(clientId);

        // 5. 刪除 BP
        deleteBPartners(clientId);

        // 6. 刪除 Price List
        deletePriceLists(clientId);

        // 7. 刪除會計架構
        deleteAccountingSchema(clientId);

        // 8. 刪除組織、倉庫
        deleteOrganizations(clientId);

        // 9. 刪除 Client
        client.deleteEx(true);

        // 10. 重置序號（可選）
        resetSequences(clientId);
    }
}
```

### 8.3 Model API 使用範例

#### 建立商品

```java
MProduct product = new MProduct(Env.getCtx(), 0, null);
product.setAD_Org_ID(0);  // 共用組織
product.setValue("PEN-BLUE");
product.setName("原子筆-藍");
product.setProductType(MProduct.PRODUCTTYPE_Item);
product.setM_Product_Category_ID(categoryId);
product.setC_UOM_ID(uomId);
product.setC_TaxCategory_ID(taxCategoryId);
product.saveEx();
```

#### 建立 Business Partner

```java
MBPartner bp = new MBPartner(Env.getCtx(), 0, null);
bp.setValue("ESLITE");
bp.setName("誠品書店");
bp.setIsCustomer(true);
bp.setIsVendor(false);
bp.setC_BP_Group_ID(customerGroupId);
bp.saveEx();
```

#### 建立 BOM

```java
// 母件
MProduct bomProduct = MProduct.get(Env.getCtx(), "BOM-GIFT-SET");
bomProduct.setIsBOM(true);
bomProduct.saveEx();

// 子件
MPPProductBOM bom = new MPPProductBOM(Env.getCtx(), 0, null);
bom.setM_Product_ID(bomProduct.getM_Product_ID());
bom.setName("文具禮盒組 BOM");
bom.setBOMType(MPPProductBOM.BOMTYPE_CurrentActive);
bom.saveEx();

// BOM Line
MPPProductBOMLine line = new MPPProductBOMLine(bom);
line.setM_Product_ID(penBlueId);
line.setQtyBOM(Env.ONE);
line.saveEx();
```

---

## 9. 建立順序

資料建立需按照相依性順序：

```
1. Client
2. Organization（依賴 Client）
3. Warehouse（依賴 Organization）
4. Locator（依賴 Warehouse）
5. Calendar、Period（依賴 Client）
6. Chart of Accounts（依賴 Client）
7. Accounting Schema（依賴 Client、CoA）
8. Product Category（依賴 Client）
9. BP Group（依賴 Client）
10. Tax（依賴 Client）
11. Payment Term（依賴 Client）
12. Price List（依賴 Client）
13. BP（依賴 BP Group）
14. Product（依賴 Product Category）
15. Product Price（依賴 Product、Price List）
16. BOM（依賴 Product）
```

---

## 10. 去重與冪等性

每次建立前先檢查是否存在：

```java
// 範例：檢查 Client 是否存在
public static boolean clientExists() {
    String sql = "SELECT AD_Client_ID FROM AD_Client WHERE Value = ?";
    int clientId = DB.getSQLValue(null, sql, "sample");
    return clientId > 0;
}

// 在 Activator.start() 中使用
if (clientExists()) {
    log.info("Sample Client 已存在，跳過初始化");
    return;
}
```

---

## 11. 錯誤處理

| 情境 | 處理方式 |
|------|----------|
| Client 已存在 | 記錄訊息，跳過初始化 |
| 建立失敗 | 使用 Transaction rollback，記錄錯誤 |
| 部分資料建立失敗 | 記錄錯誤，繼續嘗試其他資料 |
| 清理時 FK 衝突 | 先刪除相依資料，或記錄錯誤提示用戶 |

### 11.1 Transaction 管理

```java
String trxName = Trx.createTrxName("SampleSetup");
Trx trx = Trx.get(trxName, true);
try {
    // 建立資料...
    trx.commit();
} catch (Exception e) {
    trx.rollback();
    log.severe("Sample 資料建立失敗: " + e.getMessage());
} finally {
    trx.close();
}
```

---

## 12. 未來擴展

- 支援多語系（可加入英文版本）
- 支援不同行業範本（製造業、零售業等）
- 提供 UI 讓用戶選擇要建立的資料範圍
- 支援匯出為 2Pack 供離線使用

---

## 附錄 A：參考資源

| 資源 | 說明 | 連結 |
|------|------|------|
| MSetup.java | iDempiere Initial Client Setup 原始碼 | org.compiere.model.MSetup |
| Delete Client 插件 | 清理 Client 邏輯參考 | https://wiki.idempiere.org/en/Plugin:_Delete_Client_and_Initialize_Client |
| idempiere-rest | REST API 插件（可選用於檢查） | https://github.com/bxservice/idempiere-rest |
