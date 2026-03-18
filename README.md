# tw.idempiere.sample - 天地人實業示範資料插件

iDempiere 台灣本地化示範 Client 插件。安裝後自動建立「天地人實業有限公司」示範環境，包含繁體中文的會計科目、商品、客戶/供應商等基礎主檔。

## 功能特色

- **繁體中文**：所有資料皆為繁體中文，適合台灣使用者
- **台灣會計科目**：採用商業會計處理準則架構
- **完整主檔**：包含 30 種商品、15 個 BP、BOM 組合品
- **即裝即用**：安裝插件即自動建立示範資料
- **乾淨重置**：卸載插件自動清理所有資料

## 系統需求

- iDempiere 12 或以上版本
- Java 17

## 安裝方式

1. 編譯插件：
   ```bash
   mvn clean package
   ```

2. 將產生的 JAR 檔複製到 iDempiere plugins 目錄

3. 重啟 iDempiere 或透過 OSGi Console 安裝：
   ```
   install file:///path/to/tw.idempiere.sample-1.0.0-SNAPSHOT.jar
   start <bundle-id>
   ```

## 登入資訊

安裝完成後，可使用以下帳號登入：

- **Client**: 天地人實業有限公司
- **使用者**: SampleAdmin
- **密碼**: Sample123!
- **角色**: 天地人管理員

## 示範資料內容

### 組織架構

| 代碼 | 名稱 | 說明 |
|------|------|------|
| HQ | 台北總公司 | 總公司，含主要倉庫 |
| TC | 台中分公司 | 分公司，含分公司倉庫 |
| KH | 高雄倉庫 | 倉儲組織 |

### 會計架構

- 採用台灣商業會計處理準則
- 成本法：加權平均-採購 (Average PO)
- 成本層級：公司層級 (Client)
- 帳期控制：手動
- 幣別：新台幣 (TWD)

### 稅務設定

| 名稱 | 稅率 | 說明 |
|------|------|------|
| 營業稅 5% | 5% | 預設稅率 |
| 免稅 | 0% | 出口或免稅商品 |

### 商品類別

- 電子產品
- 辦公用品
- 家具
- 耗材
- 服務
- 組合品

### 業務夥伴

- 5 家供應商
- 7 家客戶
- 3 位員工

## 開發說明

### 專案結構

```
tw.idempiere.sample/
├── src/tw/idempiere/sample/
│   ├── Activator.java              # OSGi 生命週期管理
│   ├── data/                       # 資料定義
│   │   ├── BPartnerData.java       # BP 資料
│   │   ├── ChartOfAccountsTW.java  # 台灣會計科目
│   │   ├── OrganizationData.java   # 組織資料
│   │   └── ProductData.java        # 商品資料
│   ├── setup/                      # 建立邏輯
│   │   ├── SampleClientSetup.java  # 主流程控制
│   │   ├── SampleOrgSetup.java     # 組織建立
│   │   ├── SampleCalendarSetup.java# 會計年度
│   │   ├── SampleAccountingSetup.java # 會計架構
│   │   ├── SampleTaxSetup.java     # 稅務設定
│   │   ├── SamplePriceListSetup.java # 價格表
│   │   ├── SampleBPSetup.java      # 業務夥伴
│   │   └── SampleProductSetup.java # 商品
│   └── cleanup/                    # 清理邏輯
│       └── SampleClientCleanup.java
├── META-INF/MANIFEST.MF
├── OSGI-INF/component.xml
└── pom.xml
```

### 建立流程

插件啟動時會依序執行：

1. 建立 Client（天地人實業有限公司）
2. 建立組織和倉庫
3. 建立會計年度和帳期
4. 建立會計架構和科目
5. 建立稅務設定
6. 建立價格表
7. 建立業務夥伴
8. 建立商品和 BOM
9. 建立管理員角色和用戶

### 清理流程

卸載插件時會依相依性順序刪除所有資料，確保不會有 FK 違反。

## 授權條款

GNU General Public License version 2

## 貢獻者

Taiwan iDempiere Community
