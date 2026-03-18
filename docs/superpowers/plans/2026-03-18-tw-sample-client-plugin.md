# 天地人實業 iDempiere 示範資料插件 - 實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立一個 OSGi 插件，安裝時自動建立「天地人實業有限公司」示範 Client，包含台灣會計科目、商品、客戶/供應商等基礎主檔。

**Architecture:** 使用 iDempiere OSGi 插件架構，透過 Model API（MClient、MOrg、MProduct 等）建立資料。插件啟動時檢查 Client 是否存在，不存在則建立；卸載時清理所有資料。

**Tech Stack:** Java 17、OSGi、iDempiere 12 Model API、Maven

**Spec:** `docs/superpowers/specs/2026-03-18-tw-sample-client-design.md`

---

## 檔案結構

```
tw.idempiere.sample/
├── src/tw/idempiere/sample/
│   ├── Activator.java                      # OSGi 生命週期管理
│   ├── setup/
│   │   ├── SampleClientSetup.java          # Client 建立主流程
│   │   ├── SampleOrgSetup.java             # 組織/倉庫建立
│   │   ├── SampleCalendarSetup.java        # 會計年度/帳期建立
│   │   ├── SampleAccountingSetup.java      # 會計架構建立
│   │   ├── SampleTaxSetup.java             # 稅務設定
│   │   ├── SampleBPSetup.java              # Business Partner 建立
│   │   ├── SampleProductSetup.java         # 商品建立
│   │   └── SamplePriceListSetup.java       # 價格表建立
│   ├── cleanup/
│   │   └── SampleClientCleanup.java        # Client 清理
│   └── data/
│       ├── ChartOfAccountsTW.java          # 台灣會計科目定義
│       ├── OrganizationData.java           # 組織資料定義
│       ├── BPartnerData.java               # BP 資料定義
│       └── ProductData.java                # 商品資料定義
├── META-INF/
│   └── MANIFEST.MF                         # OSGi manifest
├── OSGI-INF/
│   └── component.xml                       # DS component 定義
├── pom.xml                                 # Maven 建置設定
└── README.md                               # 說明文件
```

---

## Task 1: 建立專案骨架

**Files:**
- Create: `pom.xml`
- Create: `META-INF/MANIFEST.MF`
- Create: `OSGI-INF/component.xml`
- Create: `src/tw/idempiere/sample/Activator.java`

- [ ] **Step 1: 建立 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>tw.idempiere</groupId>
    <artifactId>tw.idempiere.sample</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>iDempiere Taiwan Sample Client</name>
    <description>台灣示範資料插件 - 天地人實業有限公司</description>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <idempiere.version>12.0.0</idempiere.version>
    </properties>

    <dependencies>
        <!-- iDempiere Core -->
        <dependency>
            <groupId>org.idempiere</groupId>
            <artifactId>org.idempiere.base</artifactId>
            <version>${idempiere.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- OSGi -->
        <dependency>
            <groupId>org.osgi</groupId>
            <artifactId>org.osgi.core</artifactId>
            <version>6.0.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.osgi</groupId>
            <artifactId>org.osgi.service.component.annotations</artifactId>
            <version>1.5.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <sourceDirectory>src</sourceDirectory>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifestFile>META-INF/MANIFEST.MF</manifestFile>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 建立 MANIFEST.MF**

```
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: iDempiere Taiwan Sample Client
Bundle-SymbolicName: tw.idempiere.sample;singleton:=true
Bundle-Version: 1.0.0.qualifier
Bundle-Activator: tw.idempiere.sample.Activator
Bundle-Vendor: Taiwan iDempiere Community
Bundle-RequiredExecutionEnvironment: JavaSE-17
Require-Bundle: org.idempiere.base;bundle-version="12.0.0",
 org.adempiere.base;bundle-version="12.0.0",
 org.compiere.model;bundle-version="12.0.0"
Import-Package: org.osgi.framework;version="1.10.0",
 org.osgi.service.component.annotations;version="1.5.0"
Bundle-ActivationPolicy: lazy
Service-Component: OSGI-INF/component.xml
```

- [ ] **Step 3: 建立 component.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.4.0"
               name="tw.idempiere.sample"
               activate="activate"
               deactivate="deactivate">
    <implementation class="tw.idempiere.sample.Activator"/>
</scr:component>
```

- [ ] **Step 4: 建立 Activator.java 骨架**

```java
package tw.idempiere.sample;

import java.util.logging.Level;
import org.compiere.util.CLogger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * 天地人實業示範資料插件 - OSGi Activator
 *
 * 生命週期：
 * - start(): 檢查並建立示範 Client
 * - stop(): 保留資料（不做任何事）
 * - uninstall: 透過 BundleListener 清理資料
 */
public class Activator implements BundleActivator {

    private static final CLogger log = CLogger.getCLogger(Activator.class);
    private static final String CLIENT_VALUE = "sample";
    private BundleContext context;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        log.info("天地人實業示範資料插件啟動中...");

        // TODO: 實作 SampleClientSetup.init()
        log.info("天地人實業示範資料插件啟動完成");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("天地人實業示範資料插件停止（資料保留）");
        // 不做任何事，保留資料
    }
}
```

- [ ] **Step 5: 建立目錄結構**

```bash
mkdir -p src/tw/idempiere/sample/setup
mkdir -p src/tw/idempiere/sample/cleanup
mkdir -p src/tw/idempiere/sample/data
```

- [ ] **Step 6: Commit**

```bash
git add pom.xml META-INF/ OSGI-INF/ src/
git commit -m "chore: 建立 OSGi 插件專案骨架

- 新增 pom.xml Maven 建置設定
- 新增 MANIFEST.MF OSGi bundle 定義
- 新增 component.xml DS 元件定義
- 新增 Activator.java 骨架"
```

---

## Task 2: 實作資料定義類別

**Files:**
- Create: `src/tw/idempiere/sample/data/OrganizationData.java`
- Create: `src/tw/idempiere/sample/data/ChartOfAccountsTW.java`
- Create: `src/tw/idempiere/sample/data/BPartnerData.java`
- Create: `src/tw/idempiere/sample/data/ProductData.java`

- [ ] **Step 1: 建立 OrganizationData.java**

```java
package tw.idempiere.sample.data;

/**
 * 組織資料定義
 */
public class OrganizationData {

    public static final String[][] ORGANIZATIONS = {
        // {value, name, description, isWarehouse}
        {"taipei", "台北總公司", "總部及主要營業據點", "Y"},
        {"taichung", "台中分公司", "中部營業據點", "Y"},
        {"kaohsiung", "高雄倉庫", "南部倉儲中心", "Y"}
    };

    public static final String[][] WAREHOUSES = {
        // {orgValue, value, name}
        {"taipei", "WH-TPE", "台北主倉"},
        {"taichung", "WH-TXG", "台中倉"},
        {"kaohsiung", "WH-KHH", "高雄倉"}
    };
}
```

- [ ] **Step 2: 建立 ChartOfAccountsTW.java**

```java
package tw.idempiere.sample.data;

/**
 * 台灣商業會計處理準則 - 會計科目定義（精簡版）
 */
public class ChartOfAccountsTW {

    // 科目類型常數
    public static final String TYPE_ASSET = "A";      // 資產
    public static final String TYPE_LIABILITY = "L";  // 負債
    public static final String TYPE_EQUITY = "O";     // 權益 (Owner's Equity)
    public static final String TYPE_REVENUE = "R";    // 收入
    public static final String TYPE_EXPENSE = "E";    // 費用

    /**
     * 會計科目定義
     * {科目代碼, 科目名稱, 科目類型, 是否為彙總科目, 父科目代碼}
     */
    public static final String[][] ACCOUNTS = {
        // === 1 資產 ===
        {"1", "資產", TYPE_ASSET, "Y", null},
        {"11", "流動資產", TYPE_ASSET, "Y", "1"},
        {"1101", "現金", TYPE_ASSET, "N", "11"},
        {"1102", "銀行存款", TYPE_ASSET, "N", "11"},
        {"1103", "應收帳款", TYPE_ASSET, "N", "11"},
        {"1104", "應收票據", TYPE_ASSET, "N", "11"},
        {"1105", "存貨", TYPE_ASSET, "N", "11"},
        {"1106", "預付款項", TYPE_ASSET, "N", "11"},
        {"12", "非流動資產", TYPE_ASSET, "Y", "1"},
        {"1201", "固定資產", TYPE_ASSET, "N", "12"},
        {"1202", "累計折舊", TYPE_ASSET, "N", "12"},

        // === 2 負債 ===
        {"2", "負債", TYPE_LIABILITY, "Y", null},
        {"21", "流動負債", TYPE_LIABILITY, "Y", "2"},
        {"2101", "應付帳款", TYPE_LIABILITY, "N", "21"},
        {"2102", "應付票據", TYPE_LIABILITY, "N", "21"},
        {"2103", "應付薪資", TYPE_LIABILITY, "N", "21"},
        {"2104", "應付稅款", TYPE_LIABILITY, "N", "21"},
        {"22", "非流動負債", TYPE_LIABILITY, "Y", "2"},
        {"2201", "長期借款", TYPE_LIABILITY, "N", "22"},

        // === 3 權益 ===
        {"3", "權益", TYPE_EQUITY, "Y", null},
        {"31", "股本", TYPE_EQUITY, "Y", "3"},
        {"3101", "普通股股本", TYPE_EQUITY, "N", "31"},
        {"32", "保留盈餘", TYPE_EQUITY, "Y", "3"},
        {"3201", "法定盈餘公積", TYPE_EQUITY, "N", "32"},
        {"3202", "未分配盈餘", TYPE_EQUITY, "N", "32"},

        // === 4 收入 ===
        {"4", "收入", TYPE_REVENUE, "Y", null},
        {"41", "營業收入", TYPE_REVENUE, "Y", "4"},
        {"4101", "銷貨收入", TYPE_REVENUE, "N", "41"},
        {"4102", "銷貨退回", TYPE_REVENUE, "N", "41"},
        {"42", "營業外收入", TYPE_REVENUE, "Y", "4"},
        {"4201", "利息收入", TYPE_REVENUE, "N", "42"},

        // === 5 費用 ===
        {"5", "支出", TYPE_EXPENSE, "Y", null},
        {"51", "營業成本", TYPE_EXPENSE, "Y", "5"},
        {"5101", "銷貨成本", TYPE_EXPENSE, "N", "51"},
        {"52", "營業費用", TYPE_EXPENSE, "Y", "5"},
        {"5201", "薪資費用", TYPE_EXPENSE, "N", "52"},
        {"5202", "租金費用", TYPE_EXPENSE, "N", "52"},
        {"5203", "水電費", TYPE_EXPENSE, "N", "52"},
        {"5204", "運費", TYPE_EXPENSE, "N", "52"},
        {"53", "營業外支出", TYPE_EXPENSE, "Y", "5"},
        {"5301", "利息費用", TYPE_EXPENSE, "N", "53"}
    };

    /**
     * 預設科目對應
     * {預設科目欄位名, 科目代碼}
     */
    public static final String[][] DEFAULT_ACCOUNTS = {
        // 銷售相關
        {"P_Revenue_Acct", "4101"},           // 銷貨收入
        {"P_COGS_Acct", "5101"},              // 銷貨成本
        {"P_Asset_Acct", "1105"},             // 存貨

        // BP 相關
        {"C_Receivable_Acct", "1103"},        // 應收帳款
        {"V_Liability_Acct", "2101"},         // 應付帳款
        {"V_Prepayment_Acct", "1106"},        // 預付款項

        // 銀行相關
        {"B_Asset_Acct", "1102"},             // 銀行存款
        {"B_InTransit_Acct", "1102"},         // 在途資金

        // 稅務相關
        {"T_Due_Acct", "2104"},               // 應付稅款

        // 其他
        {"UnrealizedGain_Acct", "4201"},      // 未實現收益
        {"UnrealizedLoss_Acct", "5301"},      // 未實現損失
        {"RealizedGain_Acct", "4201"},        // 已實現收益
        {"RealizedLoss_Acct", "5301"},        // 已實現損失
        {"Retainedearning_Acct", "3202"}      // 未分配盈餘
    };
}
```

- [ ] **Step 3: 建立 BPartnerData.java**

```java
package tw.idempiere.sample.data;

/**
 * Business Partner 資料定義
 */
public class BPartnerData {

    /**
     * 供應商資料
     * {value, name, description}
     */
    public static final String[][] VENDORS = {
        {"TATUNG-STATIONERY", "大同文具股份有限公司", "主要文具供應商"},
        {"KINGCAR-LOGISTICS", "金車物流有限公司", "物流服務供應商"},
        {"VENDOR-A", "供應商 A", "一般供應商"},
        {"VENDOR-B", "供應商 B", "一般供應商"},
        {"VENDOR-C", "供應商 C", "一般供應商"}
    };

    /**
     * 客戶資料
     * {value, name, description}
     */
    public static final String[][] CUSTOMERS = {
        {"ESLITE", "誠品書店", "大型零售客戶"},
        {"FAMILY-MART", "全家便利商店", "連鎖通路客戶"},
        {"TAIPEI-GOV", "台北市政府", "政府機關客戶"},
        {"CUSTOMER-A", "客戶 A", "一般客戶"},
        {"CUSTOMER-B", "客戶 B", "一般客戶"},
        {"CUSTOMER-C", "客戶 C", "一般客戶"},
        {"CUSTOMER-D", "客戶 D", "一般客戶"}
    };

    /**
     * 員工資料
     * {value, name, description}
     */
    public static final String[][] EMPLOYEES = {
        {"EMP-WANG", "王小明", "業務人員"},
        {"EMP-LEE", "李小華", "倉管人員"},
        {"EMP-A", "員工 A", "一般員工"}
    };

    /**
     * BP 群組
     * {value, name, isDefault}
     */
    public static final String[][] BP_GROUPS = {
        {"VENDOR", "供應商", "N"},
        {"CUSTOMER", "客戶", "Y"},
        {"EMPLOYEE", "員工", "N"}
    };
}
```

- [ ] **Step 4: 建立 ProductData.java**

```java
package tw.idempiere.sample.data;

import java.math.BigDecimal;

/**
 * 商品資料定義
 */
public class ProductData {

    /**
     * 商品類別
     * {value, name}
     */
    public static final String[][] CATEGORIES = {
        {"PAPER", "紙類"},
        {"WRITING", "書寫工具"},
        {"OFFICE", "辦公用品"},
        {"FILE", "檔案用品"},
        {"SERVICE", "服務"},
        {"GENERAL", "一般商品"}
    };

    /**
     * 計量單位
     * {uomSymbol, name}
     * 注意：先檢查系統是否已有這些 UOM
     */
    public static final String[][] UOMS = {
        {"EA", "個"},
        {"PC", "支"},
        {"PK", "包"},
        {"BX", "箱"},
        {"SET", "組"},
        {"BK", "本"},
        {"RL", "捲"}
    };

    /**
     * 庫存品（Item）
     * {value, name, category, uom, purchasePrice, salesPrice}
     */
    public static final Object[][] ITEMS = {
        {"PAPER-A4-500", "A4 影印紙（500張）", "PAPER", "PK", new BigDecimal("85"), new BigDecimal("120")},
        {"PAPER-A4-BOX", "A4 影印紙（箱裝）", "PAPER", "BX", new BigDecimal("380"), new BigDecimal("500")},
        {"PEN-BLUE", "原子筆-藍", "WRITING", "PC", new BigDecimal("8"), new BigDecimal("15")},
        {"PEN-BLACK", "原子筆-黑", "WRITING", "PC", new BigDecimal("8"), new BigDecimal("15")},
        {"PEN-RED", "原子筆-紅", "WRITING", "PC", new BigDecimal("8"), new BigDecimal("15")},
        {"PENCIL-HB", "鉛筆 HB", "WRITING", "PC", new BigDecimal("5"), new BigDecimal("10")},
        {"ERASER", "橡皮擦", "WRITING", "EA", new BigDecimal("8"), new BigDecimal("15")},
        {"NOTEBOOK-25K", "筆記本-25K", "PAPER", "BK", new BigDecimal("25"), new BigDecimal("45")},
        {"NOTEBOOK-18K", "筆記本-18K", "PAPER", "BK", new BigDecimal("35"), new BigDecimal("60")},
        {"FOLDER-A4", "資料夾-A4", "FILE", "EA", new BigDecimal("15"), new BigDecimal("30")},
        {"CLIP-BOX", "迴紋針（盒裝）", "OFFICE", "BX", new BigDecimal("20"), new BigDecimal("35")},
        {"STAPLER", "釘書機", "OFFICE", "EA", new BigDecimal("80"), new BigDecimal("150")},
        {"STAPLE-BOX", "釘書針（盒裝）", "OFFICE", "BX", new BigDecimal("25"), new BigDecimal("45")},
        {"SCISSORS", "剪刀", "OFFICE", "EA", new BigDecimal("45"), new BigDecimal("85")},
        {"TAPE-DISPENSER", "膠帶台", "OFFICE", "EA", new BigDecimal("60"), new BigDecimal("120")},
        {"TAPE-CLEAR", "透明膠帶", "OFFICE", "RL", new BigDecimal("15"), new BigDecimal("30")},
        {"ITEM-A", "商品 A", "GENERAL", "EA", new BigDecimal("100"), new BigDecimal("180")},
        {"ITEM-B", "商品 B", "GENERAL", "EA", new BigDecimal("150"), new BigDecimal("280")},
        {"ITEM-C", "商品 C", "GENERAL", "EA", new BigDecimal("200"), new BigDecimal("350")},
        {"ITEM-D", "商品 D", "GENERAL", "EA", new BigDecimal("80"), new BigDecimal("150")}
    };

    /**
     * 服務（Service）
     * {value, name, category, purchasePrice, salesPrice}
     */
    public static final Object[][] SERVICES = {
        {"SVC-SHIPPING-TW", "國內運費", "SERVICE", new BigDecimal("60"), new BigDecimal("100")},
        {"SVC-INSTALL", "安裝服務", "SERVICE", new BigDecimal("300"), new BigDecimal("500")},
        {"SVC-REPAIR", "維修服務", "SERVICE", new BigDecimal("200"), new BigDecimal("400")},
        {"SVC-A", "服務 A", "SERVICE", new BigDecimal("150"), new BigDecimal("300")},
        {"SVC-B", "服務 B", "SERVICE", new BigDecimal("250"), new BigDecimal("450")}
    };

    /**
     * 組合品（BOM）
     * {value, name, category, salesPrice}
     */
    public static final Object[][] BOMS = {
        {"BOM-GIFT-SET", "文具禮盒組", "GENERAL", new BigDecimal("120")},
        {"BOM-OFFICE-SET", "辦公文具組", "OFFICE", new BigDecimal("450")},
        {"BOM-STUDENT-SET", "學生文具組", "WRITING", new BigDecimal("150")},
        {"BOM-A", "組合品 A", "GENERAL", new BigDecimal("600")},
        {"BOM-B", "組合品 B", "GENERAL", new BigDecimal("700")}
    };

    /**
     * BOM 組成
     * {bomValue, componentValue, qty}
     */
    public static final Object[][] BOM_LINES = {
        // 文具禮盒組
        {"BOM-GIFT-SET", "PEN-BLUE", new BigDecimal("1")},
        {"BOM-GIFT-SET", "NOTEBOOK-25K", new BigDecimal("1")},
        {"BOM-GIFT-SET", "FOLDER-A4", new BigDecimal("1")},

        // 辦公文具組
        {"BOM-OFFICE-SET", "STAPLER", new BigDecimal("1")},
        {"BOM-OFFICE-SET", "SCISSORS", new BigDecimal("1")},
        {"BOM-OFFICE-SET", "TAPE-DISPENSER", new BigDecimal("1")},

        // 學生文具組
        {"BOM-STUDENT-SET", "PENCIL-HB", new BigDecimal("3")},
        {"BOM-STUDENT-SET", "ERASER", new BigDecimal("1")},
        {"BOM-STUDENT-SET", "NOTEBOOK-25K", new BigDecimal("2")},

        // 組合品 A
        {"BOM-A", "ITEM-A", new BigDecimal("2")},
        {"BOM-A", "ITEM-B", new BigDecimal("1")},

        // 組合品 B
        {"BOM-B", "ITEM-C", new BigDecimal("1")},
        {"BOM-B", "ITEM-D", new BigDecimal("3")}
    };
}
```

- [ ] **Step 5: Commit**

```bash
git add src/tw/idempiere/sample/data/
git commit -m "feat: 新增資料定義類別

- OrganizationData: 組織/倉庫定義
- ChartOfAccountsTW: 台灣會計科目定義
- BPartnerData: 供應商/客戶/員工定義
- ProductData: 商品/服務/BOM 定義"
```

---

## Task 3: 實作 Client 建立基礎設定

**Files:**
- Create: `src/tw/idempiere/sample/setup/SampleClientSetup.java`
- Create: `src/tw/idempiere/sample/setup/SampleOrgSetup.java`
- Create: `src/tw/idempiere/sample/setup/SampleCalendarSetup.java`

- [ ] **Step 1: 建立 SampleClientSetup.java**

```java
package tw.idempiere.sample.setup;

import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MClient;
import org.compiere.model.MClientInfo;
import org.compiere.model.MRole;
import org.compiere.model.MUser;
import org.compiere.model.MUserRoles;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

/**
 * 天地人實業示範 Client 建立主程式
 */
public class SampleClientSetup {

    private static final CLogger log = CLogger.getCLogger(SampleClientSetup.class);

    public static final String CLIENT_VALUE = "sample";
    public static final String CLIENT_NAME = "天地人實業有限公司";

    private Properties ctx;
    private String trxName;
    private MClient client;

    public SampleClientSetup(Properties ctx, String trxName) {
        this.ctx = ctx;
        this.trxName = trxName;
    }

    /**
     * 檢查 Client 是否已存在
     */
    public static boolean clientExists() {
        String sql = "SELECT AD_Client_ID FROM AD_Client WHERE Value = ?";
        int clientId = DB.getSQLValue(null, sql, CLIENT_VALUE);
        return clientId > 0;
    }

    /**
     * 取得現有的 Client
     */
    public static MClient getExistingClient(Properties ctx) {
        String sql = "SELECT AD_Client_ID FROM AD_Client WHERE Value = ?";
        int clientId = DB.getSQLValue(null, sql, CLIENT_VALUE);
        if (clientId > 0) {
            return new MClient(ctx, clientId, null);
        }
        return null;
    }

    /**
     * 執行完整的初始化流程
     */
    public static void init() {
        if (clientExists()) {
            log.info("天地人實業 Client 已存在，跳過初始化");
            return;
        }

        String trxName = Trx.createTrxName("SampleSetup");
        Trx trx = Trx.get(trxName, true);

        try {
            Properties ctx = Env.getCtx();
            SampleClientSetup setup = new SampleClientSetup(ctx, trxName);
            setup.createClient();

            trx.commit();
            log.info("天地人實業示範資料建立完成");

        } catch (Exception e) {
            log.log(Level.SEVERE, "建立失敗", e);
            trx.rollback();
        } finally {
            trx.close();
        }
    }

    /**
     * 建立 Client 及所有相關資料
     */
    public void createClient() throws Exception {
        log.info("開始建立 Client: " + CLIENT_NAME);

        // 1. 建立 Client
        client = new MClient(ctx, 0, trxName);
        client.setValue(CLIENT_VALUE);
        client.setName(CLIENT_NAME);
        client.setDescription("iDempiere 台灣示範資料");
        client.saveEx();
        log.info("Client 建立完成: " + client.getAD_Client_ID());

        // 設定 Context
        Env.setContext(ctx, Env.AD_CLIENT_ID, client.getAD_Client_ID());

        // 2. 建立 ClientInfo
        MClientInfo clientInfo = new MClientInfo(client, 0, 0, 0, 0, 0, 0, 0, 0, null);
        clientInfo.saveEx();

        // 3. 建立組織
        SampleOrgSetup orgSetup = new SampleOrgSetup(ctx, trxName, client);
        orgSetup.createOrganizations();

        // 4. 建立會計年度和帳期
        SampleCalendarSetup calSetup = new SampleCalendarSetup(ctx, trxName, client);
        calSetup.createCalendar();

        // 5. 建立會計架構
        SampleAccountingSetup acctSetup = new SampleAccountingSetup(ctx, trxName, client);
        acctSetup.createAccountingSchema();

        // 6. 建立稅務設定
        SampleTaxSetup taxSetup = new SampleTaxSetup(ctx, trxName, client);
        taxSetup.createTax();

        // 7. 建立價格表
        SamplePriceListSetup plSetup = new SamplePriceListSetup(ctx, trxName, client);
        plSetup.createPriceLists();

        // 8. 建立 BP
        SampleBPSetup bpSetup = new SampleBPSetup(ctx, trxName, client);
        bpSetup.createBPartners();

        // 9. 建立商品
        SampleProductSetup prodSetup = new SampleProductSetup(ctx, trxName, client);
        prodSetup.createProducts();

        // 10. 建立管理員角色和用戶
        createAdminRoleAndUser();

        log.info("所有示範資料建立完成");
    }

    /**
     * 建立管理員角色和用戶
     */
    private void createAdminRoleAndUser() throws Exception {
        // 建立角色
        MRole role = new MRole(ctx, 0, trxName);
        role.setAD_Client_ID(client.getAD_Client_ID());
        role.setAD_Org_ID(0);
        role.setName("天地人管理員");
        role.setUserLevel(MRole.USERLEVEL_ClientPlusOrganization);
        role.setIsManual(false);
        role.saveEx();

        // 建立用戶
        MUser user = new MUser(ctx, 0, trxName);
        user.setAD_Client_ID(client.getAD_Client_ID());
        user.setAD_Org_ID(0);
        user.setValue("admin");
        user.setName("天地人管理員");
        user.setPassword("admin");  // 實際環境應該用更安全的密碼
        user.setIsLoginUser(true);
        user.saveEx();

        // 指派角色給用戶
        MUserRoles userRole = new MUserRoles(ctx, user.getAD_User_ID(), role.getAD_Role_ID(), trxName);
        userRole.setAD_Client_ID(client.getAD_Client_ID());
        userRole.saveEx();

        log.info("管理員角色和用戶建立完成");
    }

    public MClient getClient() {
        return client;
    }
}
```

- [ ] **Step 2: 建立 SampleOrgSetup.java**

```java
package tw.idempiere.sample.setup;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MClient;
import org.compiere.model.MLocator;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MWarehouse;
import org.compiere.util.CLogger;

import tw.idempiere.sample.data.OrganizationData;

/**
 * 組織和倉庫建立
 */
public class SampleOrgSetup {

    private static final CLogger log = CLogger.getCLogger(SampleOrgSetup.class);

    private Properties ctx;
    private String trxName;
    private MClient client;
    private Map<String, MOrg> orgMap = new HashMap<>();
    private Map<String, MWarehouse> warehouseMap = new HashMap<>();

    public SampleOrgSetup(Properties ctx, String trxName, MClient client) {
        this.ctx = ctx;
        this.trxName = trxName;
        this.client = client;
    }

    /**
     * 建立所有組織和倉庫
     */
    public void createOrganizations() throws Exception {
        log.info("開始建立組織...");

        // 建立組織
        for (String[] orgData : OrganizationData.ORGANIZATIONS) {
            String value = orgData[0];
            String name = orgData[1];
            String description = orgData[2];

            MOrg org = new MOrg(ctx, 0, trxName);
            org.setAD_Client_ID(client.getAD_Client_ID());
            org.setValue(value);
            org.setName(name);
            org.setDescription(description);
            org.saveEx();

            // 建立 OrgInfo
            MOrgInfo orgInfo = org.getInfo();
            orgInfo.saveEx();

            orgMap.put(value, org);
            log.info("組織建立完成: " + name);
        }

        // 建立倉庫
        for (String[] whData : OrganizationData.WAREHOUSES) {
            String orgValue = whData[0];
            String value = whData[1];
            String name = whData[2];

            MOrg org = orgMap.get(orgValue);
            if (org == null) continue;

            MWarehouse wh = new MWarehouse(ctx, 0, trxName);
            wh.setAD_Client_ID(client.getAD_Client_ID());
            wh.setAD_Org_ID(org.getAD_Org_ID());
            wh.setValue(value);
            wh.setName(name);
            wh.saveEx();

            // 建立預設 Locator
            MLocator locator = new MLocator(wh, name + " 預設庫位");
            locator.setValue(value + "-01");
            locator.setIsDefault(true);
            locator.saveEx();

            warehouseMap.put(value, wh);
            log.info("倉庫建立完成: " + name);
        }

        log.info("組織和倉庫建立完成，共 " + orgMap.size() + " 個組織，" + warehouseMap.size() + " 個倉庫");
    }

    public Map<String, MOrg> getOrgMap() {
        return orgMap;
    }

    public Map<String, MWarehouse> getWarehouseMap() {
        return warehouseMap;
    }
}
```

- [ ] **Step 3: 建立 SampleCalendarSetup.java**

```java
package tw.idempiere.sample.setup;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Properties;

import org.compiere.model.MCalendar;
import org.compiere.model.MClient;
import org.compiere.model.MPeriod;
import org.compiere.model.MYear;
import org.compiere.util.CLogger;

/**
 * 會計年度和帳期建立
 */
public class SampleCalendarSetup {

    private static final CLogger log = CLogger.getCLogger(SampleCalendarSetup.class);

    private Properties ctx;
    private String trxName;
    private MClient client;
    private MCalendar calendar;

    public SampleCalendarSetup(Properties ctx, String trxName, MClient client) {
        this.ctx = ctx;
        this.trxName = trxName;
        this.client = client;
    }

    /**
     * 建立會計年度和 12 個帳期
     */
    public void createCalendar() throws Exception {
        log.info("開始建立會計年度...");

        // 建立 Calendar
        calendar = new MCalendar(ctx, 0, trxName);
        calendar.setAD_Client_ID(client.getAD_Client_ID());
        calendar.setAD_Org_ID(0);
        calendar.setName("天地人會計年度");
        calendar.setDescription("天地人實業會計年度");
        calendar.saveEx();

        // 取得當年度
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // 建立 Year
        MYear year = new MYear(calendar);
        year.setFiscalYear(String.valueOf(currentYear));
        year.setDescription(currentYear + " 會計年度");
        year.saveEx();

        // 建立 12 個 Period
        for (int month = 0; month < 12; month++) {
            Calendar startCal = Calendar.getInstance();
            startCal.set(currentYear, month, 1, 0, 0, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            Calendar endCal = Calendar.getInstance();
            endCal.set(currentYear, month, startCal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
            endCal.set(Calendar.MILLISECOND, 999);

            String periodName = String.format("%d-%02d", currentYear, month + 1);

            MPeriod period = new MPeriod(year, month + 1,
                new Timestamp(startCal.getTimeInMillis()),
                new Timestamp(endCal.getTimeInMillis()),
                periodName);
            period.saveEx();

            log.info("帳期建立完成: " + periodName);
        }

        // 更新 Client 設定：關閉 Automatic Period Control
        client.setIsUseBetaFunctions(false);
        client.saveEx();

        log.info("會計年度建立完成，共 12 個帳期");
    }

    public MCalendar getCalendar() {
        return calendar;
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/tw/idempiere/sample/setup/SampleClientSetup.java
git add src/tw/idempiere/sample/setup/SampleOrgSetup.java
git add src/tw/idempiere/sample/setup/SampleCalendarSetup.java
git commit -m "feat: 實作 Client、組織、會計年度建立

- SampleClientSetup: Client 建立主流程
- SampleOrgSetup: 組織和倉庫建立
- SampleCalendarSetup: 會計年度和帳期建立"
```

---

## Task 4: 實作會計架構和稅務設定

**Files:**
- Create: `src/tw/idempiere/sample/setup/SampleAccountingSetup.java`
- Create: `src/tw/idempiere/sample/setup/SampleTaxSetup.java`

- [ ] **Step 1: 建立 SampleAccountingSetup.java**

```java
package tw.idempiere.sample.setup;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaDefault;
import org.compiere.model.MClient;
import org.compiere.model.MElement;
import org.compiere.model.MElementValue;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import tw.idempiere.sample.data.ChartOfAccountsTW;

/**
 * 會計架構建立
 */
public class SampleAccountingSetup {

    private static final CLogger log = CLogger.getCLogger(SampleAccountingSetup.class);

    // TWD Currency ID (假設已存在於系統)
    private static final int TWD_CURRENCY_ID = 148;

    private Properties ctx;
    private String trxName;
    private MClient client;
    private MElement element;
    private MAcctSchema acctSchema;
    private Map<String, MElementValue> accountMap = new HashMap<>();

    public SampleAccountingSetup(Properties ctx, String trxName, MClient client) {
        this.ctx = ctx;
        this.trxName = trxName;
        this.client = client;
    }

    /**
     * 建立完整的會計架構
     */
    public void createAccountingSchema() throws Exception {
        log.info("開始建立會計架構...");

        // 1. 建立 Element（科目表）
        createElement();

        // 2. 建立 ElementValue（各科目）
        createAccounts();

        // 3. 建立 Accounting Schema
        createAcctSchema();

        // 4. 設定 Default Accounts
        setupDefaultAccounts();

        log.info("會計架構建立完成");
    }

    /**
     * 建立 Element
     */
    private void createElement() throws Exception {
        element = new MElement(ctx, 0, trxName);
        element.setAD_Client_ID(client.getAD_Client_ID());
        element.setAD_Org_ID(0);
        element.setName("台灣會計科目");
        element.setDescription("台灣商業會計處理準則");
        element.setElementType(MElement.ELEMENTTYPE_Account);
        element.setIsNaturalAccount(true);
        element.saveEx();

        log.info("Element 建立完成: " + element.getName());
    }

    /**
     * 建立所有會計科目
     */
    private void createAccounts() throws Exception {
        log.info("開始建立會計科目...");

        // 第一輪：建立所有科目（不設定父科目）
        for (String[] acctData : ChartOfAccountsTW.ACCOUNTS) {
            String value = acctData[0];
            String name = acctData[1];
            String type = acctData[2];
            boolean isSummary = "Y".equals(acctData[3]);

            MElementValue ev = new MElementValue(ctx, 0, trxName);
            ev.setAD_Client_ID(client.getAD_Client_ID());
            ev.setAD_Org_ID(0);
            ev.setC_Element_ID(element.getC_Element_ID());
            ev.setValue(value);
            ev.setName(name);
            ev.setAccountType(type);
            ev.setIsSummary(isSummary);
            ev.setIsDocControlled(false);
            ev.setPostActual(true);
            ev.setPostBudget(true);
            ev.setPostStatistical(false);
            ev.saveEx();

            accountMap.put(value, ev);
        }

        // 第二輪：設定父科目關係
        for (String[] acctData : ChartOfAccountsTW.ACCOUNTS) {
            String value = acctData[0];
            String parentValue = acctData[4];

            if (parentValue != null) {
                MElementValue ev = accountMap.get(value);
                MElementValue parent = accountMap.get(parentValue);
                if (ev != null && parent != null) {
                    ev.setParent_ID(parent.getC_ElementValue_ID());
                    ev.saveEx();
                }
            }
        }

        log.info("會計科目建立完成，共 " + accountMap.size() + " 個科目");
    }

    /**
     * 建立 Accounting Schema
     */
    private void createAcctSchema() throws Exception {
        acctSchema = new MAcctSchema(ctx, 0, trxName);
        acctSchema.setAD_Client_ID(client.getAD_Client_ID());
        acctSchema.setAD_Org_ID(0);
        acctSchema.setName("天地人會計架構");
        acctSchema.setC_Currency_ID(getTWDCurrencyId());
        acctSchema.setC_Element_ID(element.getC_Element_ID());
        acctSchema.setCostingMethod(MAcctSchema.COSTINGMETHOD_AveragePO);
        acctSchema.setCostingLevel(MAcctSchema.COSTINGLEVEL_Client);
        acctSchema.setIsAccrual(true);
        acctSchema.setAutoPeriodControl(false);  // 關閉自動帳期控制
        acctSchema.setGAAP(MAcctSchema.GAAP_InternationalGAAP);
        acctSchema.saveEx();

        log.info("Accounting Schema 建立完成: " + acctSchema.getName());
    }

    /**
     * 設定 Default Accounts
     */
    private void setupDefaultAccounts() throws Exception {
        MAcctSchemaDefault defaults = new MAcctSchemaDefault(ctx, 0, trxName);
        defaults.setAD_Client_ID(client.getAD_Client_ID());
        defaults.setAD_Org_ID(0);
        defaults.setC_AcctSchema_ID(acctSchema.getC_AcctSchema_ID());

        // 設定各預設科目
        for (String[] mapping : ChartOfAccountsTW.DEFAULT_ACCOUNTS) {
            String fieldName = mapping[0];
            String acctValue = mapping[1];

            MElementValue ev = accountMap.get(acctValue);
            if (ev != null) {
                int validCombId = createValidCombination(ev);
                setDefaultAccount(defaults, fieldName, validCombId);
            }
        }

        defaults.saveEx();
        log.info("Default Accounts 設定完成");
    }

    /**
     * 建立 Valid Combination
     */
    private int createValidCombination(MElementValue ev) {
        // 簡化版：直接用 SQL 建立
        // 實際應該用 MAccount.get() 或類似方法
        String sql = "INSERT INTO C_ValidCombination " +
            "(C_ValidCombination_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy, " +
            "Combination, Description, IsFullyQualified, C_AcctSchema_ID, Account_ID) " +
            "VALUES (?, ?, 0, 'Y', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, ?, ?, 'Y', ?, ?)";

        int id = DB.getNextID(client.getAD_Client_ID(), "C_ValidCombination", trxName);
        DB.executeUpdate(sql, new Object[]{
            id,
            client.getAD_Client_ID(),
            ev.getValue() + " " + ev.getName(),
            ev.getName(),
            acctSchema.getC_AcctSchema_ID(),
            ev.getC_ElementValue_ID()
        }, false, trxName);

        return id;
    }

    /**
     * 設定 Default Account 欄位
     */
    private void setDefaultAccount(MAcctSchemaDefault defaults, String fieldName, int validCombId) {
        try {
            java.lang.reflect.Method method = defaults.getClass().getMethod(
                "set" + fieldName, int.class);
            method.invoke(defaults, validCombId);
        } catch (Exception e) {
            log.warning("無法設定欄位 " + fieldName + ": " + e.getMessage());
        }
    }

    /**
     * 取得 TWD Currency ID
     */
    private int getTWDCurrencyId() {
        String sql = "SELECT C_Currency_ID FROM C_Currency WHERE ISO_Code = 'TWD'";
        int id = DB.getSQLValue(trxName, sql);
        if (id <= 0) {
            // 如果 TWD 不存在，使用系統預設
            sql = "SELECT C_Currency_ID FROM C_Currency WHERE IsDefault = 'Y' AND AD_Client_ID = 0";
            id = DB.getSQLValue(trxName, sql);
        }
        return id > 0 ? id : 100;  // fallback to USD
    }

    public MAcctSchema getAcctSchema() {
        return acctSchema;
    }

    public Map<String, MElementValue> getAccountMap() {
        return accountMap;
    }
}
```

- [ ] **Step 2: 建立 SampleTaxSetup.java**

```java
package tw.idempiere.sample.setup;

import java.math.BigDecimal;
import java.util.Properties;

import org.compiere.model.MClient;
import org.compiere.model.MTax;
import org.compiere.model.MTaxCategory;
import org.compiere.util.CLogger;

/**
 * 稅務設定建立
 */
public class SampleTaxSetup {

    private static final CLogger log = CLogger.getCLogger(SampleTaxSetup.class);

    private Properties ctx;
    private String trxName;
    private MClient client;
    private MTaxCategory taxCategory;
    private MTax tax;

    public SampleTaxSetup(Properties ctx, String trxName, MClient client) {
        this.ctx = ctx;
        this.trxName = trxName;
        this.client = client;
    }

    /**
     * 建立稅務設定
     */
    public void createTax() throws Exception {
        log.info("開始建立稅務設定...");

        // 建立 Tax Category
        taxCategory = new MTaxCategory(ctx, 0, trxName);
        taxCategory.setAD_Client_ID(client.getAD_Client_ID());
        taxCategory.setAD_Org_ID(0);
        taxCategory.setName("台灣營業稅");
        taxCategory.setDescription("台灣營業稅分類");
        taxCategory.setIsDefault(true);
        taxCategory.saveEx();

        // 建立營業稅 5%
        tax = new MTax(ctx, 0, trxName);
        tax.setAD_Client_ID(client.getAD_Client_ID());
        tax.setAD_Org_ID(0);
        tax.setName("營業稅 5%");
        tax.setDescription("台灣營業稅");
        tax.setC_TaxCategory_ID(taxCategory.getC_TaxCategory_ID());
        tax.setRate(new BigDecimal("5"));
        tax.setIsDefault(true);
        tax.setIsSummary(false);
        tax.setIsDocumentLevel(true);
        tax.setSOPOType(MTax.SOPOTYPE_Both);
        tax.setValidFrom(new java.sql.Timestamp(System.currentTimeMillis()));
        tax.saveEx();

        // 建立免稅
        MTax taxExempt = new MTax(ctx, 0, trxName);
        taxExempt.setAD_Client_ID(client.getAD_Client_ID());
        taxExempt.setAD_Org_ID(0);
        taxExempt.setName("免稅");
        taxExempt.setDescription("免稅");
        taxExempt.setC_TaxCategory_ID(taxCategory.getC_TaxCategory_ID());
        taxExempt.setRate(BigDecimal.ZERO);
        taxExempt.setIsDefault(false);
        taxExempt.setIsSummary(false);
        taxExempt.setIsDocumentLevel(true);
        taxExempt.setSOPOType(MTax.SOPOTYPE_Both);
        taxExempt.setValidFrom(new java.sql.Timestamp(System.currentTimeMillis()));
        taxExempt.saveEx();

        log.info("稅務設定建立完成");
    }

    public MTaxCategory getTaxCategory() {
        return taxCategory;
    }

    public MTax getTax() {
        return tax;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/tw/idempiere/sample/setup/SampleAccountingSetup.java
git add src/tw/idempiere/sample/setup/SampleTaxSetup.java
git commit -m "feat: 實作會計架構和稅務設定

- SampleAccountingSetup: 會計科目、Accounting Schema
- SampleTaxSetup: 營業稅 5%、免稅設定"
```

---

## Task 5: 實作 BP 和商品建立

**Files:**
- Create: `src/tw/idempiere/sample/setup/SampleBPSetup.java`
- Create: `src/tw/idempiere/sample/setup/SampleProductSetup.java`
- Create: `src/tw/idempiere/sample/setup/SamplePriceListSetup.java`

- [ ] **Step 1: 建立 SamplePriceListSetup.java**

```java
package tw.idempiere.sample.setup;

import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.MClient;
import org.compiere.model.MPriceList;
import org.compiere.model.MPriceListVersion;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

/**
 * 價格表建立
 */
public class SamplePriceListSetup {

    private static final CLogger log = CLogger.getCLogger(SamplePriceListSetup.class);

    private Properties ctx;
    private String trxName;
    private MClient client;
    private MPriceList salesPriceList;
    private MPriceList purchasePriceList;
    private MPriceListVersion salesPLV;
    private MPriceListVersion purchasePLV;

    public SamplePriceListSetup(Properties ctx, String trxName, MClient client) {
        this.ctx = ctx;
        this.trxName = trxName;
        this.client = client;
    }

    /**
     * 建立價格表
     */
    public void createPriceLists() throws Exception {
        log.info("開始建立價格表...");

        int currencyId = getTWDCurrencyId();

        // 銷售價格表
        salesPriceList = new MPriceList(ctx, 0, trxName);
        salesPriceList.setAD_Client_ID(client.getAD_Client_ID());
        salesPriceList.setAD_Org_ID(0);
        salesPriceList.setName("標準售價");
        salesPriceList.setDescription("銷售用價格表");
        salesPriceList.setC_Currency_ID(currencyId);
        salesPriceList.setIsSOPriceList(true);
        salesPriceList.setIsDefault(true);
        salesPriceList.setIsTaxIncluded(true);  // 含稅價
        salesPriceList.saveEx();

        // 銷售價格表版本
        salesPLV = new MPriceListVersion(salesPriceList);
        salesPLV.setName("標準售價 " + new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()));
        salesPLV.setValidFrom(new Timestamp(System.currentTimeMillis()));
        salesPLV.saveEx();

        // 採購價格表
        purchasePriceList = new MPriceList(ctx, 0, trxName);
        purchasePriceList.setAD_Client_ID(client.getAD_Client_ID());
        purchasePriceList.setAD_Org_ID(0);
        purchasePriceList.setName("標準進價");
        purchasePriceList.setDescription("採購用價格表");
        purchasePriceList.setC_Currency_ID(currencyId);
        purchasePriceList.setIsSOPriceList(false);
        purchasePriceList.setIsDefault(true);
        purchasePriceList.setIsTaxIncluded(true);
        purchasePriceList.saveEx();

        // 採購價格表版本
        purchasePLV = new MPriceListVersion(purchasePriceList);
        purchasePLV.setName("標準進價 " + new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()));
        purchasePLV.setValidFrom(new Timestamp(System.currentTimeMillis()));
        purchasePLV.saveEx();

        log.info("價格表建立完成");
    }

    private int getTWDCurrencyId() {
        String sql = "SELECT C_Currency_ID FROM C_Currency WHERE ISO_Code = 'TWD'";
        int id = DB.getSQLValue(trxName, sql);
        return id > 0 ? id : 100;
    }

    public MPriceList getSalesPriceList() {
        return salesPriceList;
    }

    public MPriceList getPurchasePriceList() {
        return purchasePriceList;
    }

    public MPriceListVersion getSalesPLV() {
        return salesPLV;
    }

    public MPriceListVersion getPurchasePLV() {
        return purchasePLV;
    }
}
```

- [ ] **Step 2: 建立 SampleBPSetup.java**

```java
package tw.idempiere.sample.setup;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MBPGroup;
import org.compiere.model.MBPartner;
import org.compiere.model.MClient;
import org.compiere.util.CLogger;

import tw.idempiere.sample.data.BPartnerData;

/**
 * Business Partner 建立
 */
public class SampleBPSetup {

    private static final CLogger log = CLogger.getCLogger(SampleBPSetup.class);

    private Properties ctx;
    private String trxName;
    private MClient client;
    private Map<String, MBPGroup> groupMap = new HashMap<>();
    private Map<String, MBPartner> bpMap = new HashMap<>();

    public SampleBPSetup(Properties ctx, String trxName, MClient client) {
        this.ctx = ctx;
        this.trxName = trxName;
        this.client = client;
    }

    /**
     * 建立所有 BP
     */
    public void createBPartners() throws Exception {
        log.info("開始建立 Business Partners...");

        // 建立 BP Group
        createBPGroups();

        // 建立供應商
        for (String[] data : BPartnerData.VENDORS) {
            createBPartner(data[0], data[1], data[2], false, true, false, "VENDOR");
        }

        // 建立客戶
        for (String[] data : BPartnerData.CUSTOMERS) {
            createBPartner(data[0], data[1], data[2], true, false, false, "CUSTOMER");
        }

        // 建立員工
        for (String[] data : BPartnerData.EMPLOYEES) {
            createBPartner(data[0], data[1], data[2], false, false, true, "EMPLOYEE");
        }

        log.info("Business Partners 建立完成，共 " + bpMap.size() + " 筆");
    }

    /**
     * 建立 BP Groups
     */
    private void createBPGroups() throws Exception {
        for (String[] data : BPartnerData.BP_GROUPS) {
            MBPGroup group = new MBPGroup(ctx, 0, trxName);
            group.setAD_Client_ID(client.getAD_Client_ID());
            group.setAD_Org_ID(0);
            group.setValue(data[0]);
            group.setName(data[1]);
            group.setIsDefault("Y".equals(data[2]));
            group.saveEx();

            groupMap.put(data[0], group);
        }
        log.info("BP Groups 建立完成，共 " + groupMap.size() + " 個");
    }

    /**
     * 建立單一 BP
     */
    private void createBPartner(String value, String name, String description,
                                boolean isCustomer, boolean isVendor, boolean isEmployee,
                                String groupValue) throws Exception {

        MBPGroup group = groupMap.get(groupValue);

        MBPartner bp = new MBPartner(ctx, 0, trxName);
        bp.setAD_Client_ID(client.getAD_Client_ID());
        bp.setAD_Org_ID(0);
        bp.setValue(value);
        bp.setName(name);
        bp.setDescription(description);
        bp.setIsCustomer(isCustomer);
        bp.setIsVendor(isVendor);
        bp.setIsEmployee(isEmployee);
        if (group != null) {
            bp.setC_BP_Group_ID(group.getC_BP_Group_ID());
        }
        bp.saveEx();

        bpMap.put(value, bp);
    }

    public Map<String, MBPartner> getBPMap() {
        return bpMap;
    }
}
```

- [ ] **Step 3: 建立 SampleProductSetup.java**

```java
package tw.idempiere.sample.setup;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MClient;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MProductPrice;
import org.compiere.model.MUOM;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;

import tw.idempiere.sample.data.ProductData;

/**
 * 商品建立
 */
public class SampleProductSetup {

    private static final CLogger log = CLogger.getCLogger(SampleProductSetup.class);

    private Properties ctx;
    private String trxName;
    private MClient client;
    private Map<String, MProductCategory> categoryMap = new HashMap<>();
    private Map<String, Integer> uomMap = new HashMap<>();
    private Map<String, MProduct> productMap = new HashMap<>();
    private int taxCategoryId;
    private MPriceListVersion salesPLV;
    private MPriceListVersion purchasePLV;

    public SampleProductSetup(Properties ctx, String trxName, MClient client) {
        this.ctx = ctx;
        this.trxName = trxName;
        this.client = client;
    }

    public void setSalesPLV(MPriceListVersion salesPLV) {
        this.salesPLV = salesPLV;
    }

    public void setPurchasePLV(MPriceListVersion purchasePLV) {
        this.purchasePLV = purchasePLV;
    }

    public void setTaxCategoryId(int taxCategoryId) {
        this.taxCategoryId = taxCategoryId;
    }

    /**
     * 建立所有商品
     */
    public void createProducts() throws Exception {
        log.info("開始建立商品...");

        // 1. 建立商品類別
        createCategories();

        // 2. 取得/建立 UOM
        setupUOMs();

        // 3. 建立庫存品
        for (Object[] data : ProductData.ITEMS) {
            createProduct(
                (String) data[0],  // value
                (String) data[1],  // name
                (String) data[2],  // category
                (String) data[3],  // uom
                MProduct.PRODUCTTYPE_Item,
                (BigDecimal) data[4],  // purchase price
                (BigDecimal) data[5]   // sales price
            );
        }

        // 4. 建立服務
        for (Object[] data : ProductData.SERVICES) {
            createProduct(
                (String) data[0],
                (String) data[1],
                (String) data[2],
                "EA",  // 服務用個
                MProduct.PRODUCTTYPE_Service,
                (BigDecimal) data[3],
                (BigDecimal) data[4]
            );
        }

        // 5. 建立 BOM 母件
        for (Object[] data : ProductData.BOMS) {
            MProduct product = createProduct(
                (String) data[0],
                (String) data[1],
                (String) data[2],
                "SET",  // 組合品用組
                MProduct.PRODUCTTYPE_Item,
                null,
                (BigDecimal) data[3]
            );
            product.setIsBOM(true);
            product.saveEx();
        }

        // 6. 建立 BOM 結構
        createBOMs();

        log.info("商品建立完成，共 " + productMap.size() + " 項");
    }

    /**
     * 建立商品類別
     */
    private void createCategories() throws Exception {
        for (String[] data : ProductData.CATEGORIES) {
            MProductCategory cat = new MProductCategory(ctx, 0, trxName);
            cat.setAD_Client_ID(client.getAD_Client_ID());
            cat.setAD_Org_ID(0);
            cat.setValue(data[0]);
            cat.setName(data[1]);
            cat.saveEx();

            categoryMap.put(data[0], cat);
        }
        log.info("商品類別建立完成，共 " + categoryMap.size() + " 個");
    }

    /**
     * 設定 UOM（使用系統既有的或建立新的）
     */
    private void setupUOMs() {
        for (String[] data : ProductData.UOMS) {
            String symbol = data[0];
            String name = data[1];

            // 先查詢系統是否已有此 UOM
            String sql = "SELECT C_UOM_ID FROM C_UOM WHERE UOMSymbol = ? OR X12DE355 = ?";
            int uomId = DB.getSQLValue(trxName, sql, symbol, symbol);

            if (uomId <= 0) {
                // 建立新 UOM
                MUOM uom = new MUOM(ctx, 0, trxName);
                uom.setAD_Client_ID(0);  // UOM 通常是系統層級
                uom.setAD_Org_ID(0);
                uom.setUOMSymbol(symbol);
                uom.setX12DE355(symbol);
                uom.setName(name);
                uom.saveEx();
                uomId = uom.getC_UOM_ID();
            }

            uomMap.put(symbol, uomId);
        }
        log.info("UOM 設定完成");
    }

    /**
     * 建立單一商品
     */
    private MProduct createProduct(String value, String name, String categoryValue,
                                   String uomSymbol, String productType,
                                   BigDecimal purchasePrice, BigDecimal salesPrice) throws Exception {

        MProductCategory category = categoryMap.get(categoryValue);
        Integer uomId = uomMap.get(uomSymbol);
        if (uomId == null) uomId = uomMap.get("EA");

        MProduct product = new MProduct(ctx, 0, trxName);
        product.setAD_Client_ID(client.getAD_Client_ID());
        product.setAD_Org_ID(0);
        product.setValue(value);
        product.setName(name);
        product.setProductType(productType);
        product.setC_UOM_ID(uomId);
        if (category != null) {
            product.setM_Product_Category_ID(category.getM_Product_Category_ID());
        }
        product.setC_TaxCategory_ID(taxCategoryId);
        product.setIsPurchased(true);
        product.setIsSold(true);
        product.setIsStocked(MProduct.PRODUCTTYPE_Item.equals(productType));
        product.saveEx();

        // 建立價格
        if (salesPrice != null && salesPLV != null) {
            createProductPrice(product, salesPLV, salesPrice);
        }
        if (purchasePrice != null && purchasePLV != null) {
            createProductPrice(product, purchasePLV, purchasePrice);
        }

        productMap.put(value, product);
        return product;
    }

    /**
     * 建立商品價格
     */
    private void createProductPrice(MProduct product, MPriceListVersion plv, BigDecimal price) {
        MProductPrice pp = new MProductPrice(ctx, 0, trxName);
        pp.setM_PriceList_Version_ID(plv.getM_PriceList_Version_ID());
        pp.setM_Product_ID(product.getM_Product_ID());
        pp.setPriceList(price);
        pp.setPriceStd(price);
        pp.setPriceLimit(price);
        pp.saveEx();
    }

    /**
     * 建立 BOM 結構
     */
    private void createBOMs() throws Exception {
        log.info("開始建立 BOM...");

        // 依據 BOM 母件分組
        Map<String, MPPProductBOM> bomMap = new HashMap<>();

        for (Object[] data : ProductData.BOM_LINES) {
            String bomValue = (String) data[0];
            String componentValue = (String) data[1];
            BigDecimal qty = (BigDecimal) data[2];

            MProduct bomProduct = productMap.get(bomValue);
            MProduct component = productMap.get(componentValue);

            if (bomProduct == null || component == null) continue;

            // 取得或建立 BOM Header
            MPPProductBOM bom = bomMap.get(bomValue);
            if (bom == null) {
                bom = new MPPProductBOM(ctx, 0, trxName);
                bom.setAD_Client_ID(client.getAD_Client_ID());
                bom.setAD_Org_ID(0);
                bom.setM_Product_ID(bomProduct.getM_Product_ID());
                bom.setValue(bomValue);
                bom.setName(bomProduct.getName() + " BOM");
                bom.setBOMType(MPPProductBOM.BOMTYPE_CurrentActive);
                bom.setBOMUse(MPPProductBOM.BOMUSE_Master);
                bom.setC_UOM_ID(bomProduct.getC_UOM_ID());
                bom.saveEx();
                bomMap.put(bomValue, bom);
            }

            // 建立 BOM Line
            MPPProductBOMLine line = new MPPProductBOMLine(bom);
            line.setM_Product_ID(component.getM_Product_ID());
            line.setQtyBOM(qty);
            line.setC_UOM_ID(component.getC_UOM_ID());
            line.saveEx();
        }

        log.info("BOM 建立完成，共 " + bomMap.size() + " 組");
    }

    public Map<String, MProduct> getProductMap() {
        return productMap;
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/tw/idempiere/sample/setup/SamplePriceListSetup.java
git add src/tw/idempiere/sample/setup/SampleBPSetup.java
git add src/tw/idempiere/sample/setup/SampleProductSetup.java
git commit -m "feat: 實作 BP、商品、價格表建立

- SamplePriceListSetup: 銷售/採購價格表
- SampleBPSetup: 供應商/客戶/員工
- SampleProductSetup: 商品/服務/BOM"
```

---

## Task 6: 實作清理功能

**Files:**
- Create: `src/tw/idempiere/sample/cleanup/SampleClientCleanup.java`
- Modify: `src/tw/idempiere/sample/Activator.java`

- [ ] **Step 1: 建立 SampleClientCleanup.java**

```java
package tw.idempiere.sample.cleanup;

import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MClient;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import tw.idempiere.sample.setup.SampleClientSetup;

/**
 * 天地人實業示範 Client 清理
 * 參考 Delete Client 插件的邏輯
 */
public class SampleClientCleanup {

    private static final CLogger log = CLogger.getCLogger(SampleClientCleanup.class);

    /**
     * 執行清理
     */
    public static void cleanup() {
        MClient client = SampleClientSetup.getExistingClient(Env.getCtx());
        if (client == null) {
            log.info("天地人實業 Client 不存在，無需清理");
            return;
        }

        int clientId = client.getAD_Client_ID();
        log.info("開始清理天地人實業 Client (ID=" + clientId + ")...");

        String trxName = Trx.createTrxName("SampleCleanup");
        Trx trx = Trx.get(trxName, true);

        try {
            Properties ctx = Env.getCtx();

            // 依相依性順序刪除
            // 1. 刪除交易資料（若有）
            deleteTransactions(clientId, trxName);

            // 2. 刪除 BOM
            deleteBOMs(clientId, trxName);

            // 3. 刪除 Product Price
            deleteProductPrices(clientId, trxName);

            // 4. 刪除 Product
            deleteProducts(clientId, trxName);

            // 5. 刪除 BP
            deleteBPartners(clientId, trxName);

            // 6. 刪除 BP Group
            deleteBPGroups(clientId, trxName);

            // 7. 刪除 Product Category
            deleteProductCategories(clientId, trxName);

            // 8. 刪除 Price List
            deletePriceLists(clientId, trxName);

            // 9. 刪除 Tax
            deleteTax(clientId, trxName);

            // 10. 刪除 Accounting Schema
            deleteAccountingSchema(clientId, trxName);

            // 11. 刪除 Calendar
            deleteCalendar(clientId, trxName);

            // 12. 刪除 Warehouse, Locator
            deleteWarehouses(clientId, trxName);

            // 13. 刪除 Organization
            deleteOrganizations(clientId, trxName);

            // 14. 刪除 User, Role
            deleteUsersAndRoles(clientId, trxName);

            // 15. 刪除 ClientInfo
            deleteClientInfo(clientId, trxName);

            // 16. 刪除 Client
            deleteClient(clientId, trxName);

            trx.commit();
            log.info("天地人實業 Client 清理完成");

        } catch (Exception e) {
            log.log(Level.SEVERE, "清理失敗", e);
            trx.rollback();
        } finally {
            trx.close();
        }
    }

    private static void deleteTransactions(int clientId, String trxName) {
        // 刪除可能的交易資料
        String[] tables = {
            "C_InvoiceLine", "C_Invoice",
            "M_InOutLine", "M_InOut",
            "C_OrderLine", "C_Order",
            "M_MovementLine", "M_Movement",
            "M_InventoryLine", "M_Inventory",
            "C_Payment", "C_BankStatementLine", "C_BankStatement",
            "Fact_Acct", "GL_JournalLine", "GL_Journal", "GL_JournalBatch"
        };

        for (String table : tables) {
            deleteFromTable(table, clientId, trxName);
        }
    }

    private static void deleteBOMs(int clientId, String trxName) {
        deleteFromTable("PP_Product_BOMLine", clientId, trxName);
        deleteFromTable("PP_Product_BOM", clientId, trxName);
    }

    private static void deleteProductPrices(int clientId, String trxName) {
        DB.executeUpdate("DELETE FROM M_ProductPrice WHERE M_PriceList_Version_ID IN " +
            "(SELECT M_PriceList_Version_ID FROM M_PriceList_Version WHERE M_PriceList_ID IN " +
            "(SELECT M_PriceList_ID FROM M_PriceList WHERE AD_Client_ID = ?))",
            new Object[]{clientId}, false, trxName);
    }

    private static void deleteProducts(int clientId, String trxName) {
        // 先刪除 Product Acct
        deleteFromTable("M_Product_Acct", clientId, trxName);
        deleteFromTable("M_Product", clientId, trxName);
    }

    private static void deleteBPartners(int clientId, String trxName) {
        deleteFromTable("C_BP_BankAccount", clientId, trxName);
        deleteFromTable("C_BPartner_Location", clientId, trxName);
        deleteFromTable("AD_User", clientId, trxName);  // BP 的 Contact
        deleteFromTable("C_BPartner", clientId, trxName);
    }

    private static void deleteBPGroups(int clientId, String trxName) {
        deleteFromTable("C_BP_Group_Acct", clientId, trxName);
        deleteFromTable("C_BP_Group", clientId, trxName);
    }

    private static void deleteProductCategories(int clientId, String trxName) {
        deleteFromTable("M_Product_Category_Acct", clientId, trxName);
        deleteFromTable("M_Product_Category", clientId, trxName);
    }

    private static void deletePriceLists(int clientId, String trxName) {
        deleteFromTable("M_PriceList_Version", clientId, trxName);
        deleteFromTable("M_PriceList", clientId, trxName);
    }

    private static void deleteTax(int clientId, String trxName) {
        deleteFromTable("C_Tax_Acct", clientId, trxName);
        deleteFromTable("C_Tax", clientId, trxName);
        deleteFromTable("C_TaxCategory", clientId, trxName);
    }

    private static void deleteAccountingSchema(int clientId, String trxName) {
        deleteFromTable("C_AcctSchema_Default", clientId, trxName);
        deleteFromTable("C_AcctSchema_Element", clientId, trxName);
        deleteFromTable("C_AcctSchema_GL", clientId, trxName);
        deleteFromTable("C_ValidCombination", clientId, trxName);
        deleteFromTable("C_AcctSchema", clientId, trxName);
        deleteFromTable("C_ElementValue", clientId, trxName);
        deleteFromTable("C_Element", clientId, trxName);
    }

    private static void deleteCalendar(int clientId, String trxName) {
        deleteFromTable("C_PeriodControl", clientId, trxName);
        deleteFromTable("C_Period", clientId, trxName);
        deleteFromTable("C_Year", clientId, trxName);
        deleteFromTable("C_Calendar", clientId, trxName);
    }

    private static void deleteWarehouses(int clientId, String trxName) {
        deleteFromTable("M_Locator", clientId, trxName);
        deleteFromTable("M_Warehouse_Acct", clientId, trxName);
        deleteFromTable("M_Warehouse", clientId, trxName);
    }

    private static void deleteOrganizations(int clientId, String trxName) {
        // 先刪除非 * 組織
        DB.executeUpdate("DELETE FROM AD_OrgInfo WHERE AD_Org_ID IN " +
            "(SELECT AD_Org_ID FROM AD_Org WHERE AD_Client_ID = ? AND AD_Org_ID > 0)",
            new Object[]{clientId}, false, trxName);
        DB.executeUpdate("DELETE FROM AD_Org WHERE AD_Client_ID = ? AND AD_Org_ID > 0",
            new Object[]{clientId}, false, trxName);
    }

    private static void deleteUsersAndRoles(int clientId, String trxName) {
        deleteFromTable("AD_User_Roles", clientId, trxName);
        deleteFromTable("AD_Role_OrgAccess", clientId, trxName);
        deleteFromTable("AD_Window_Access", clientId, trxName);
        deleteFromTable("AD_Process_Access", clientId, trxName);
        deleteFromTable("AD_Form_Access", clientId, trxName);
        deleteFromTable("AD_Task_Access", clientId, trxName);
        deleteFromTable("AD_Workflow_Access", clientId, trxName);
        deleteFromTable("AD_Role", clientId, trxName);
        // User 已在 deleteBPartners 中刪除
    }

    private static void deleteClientInfo(int clientId, String trxName) {
        deleteFromTable("AD_ClientInfo", clientId, trxName);
    }

    private static void deleteClient(int clientId, String trxName) {
        DB.executeUpdate("DELETE FROM AD_Client WHERE AD_Client_ID = ?",
            new Object[]{clientId}, false, trxName);
        log.info("Client 刪除完成: " + clientId);
    }

    /**
     * 通用刪除方法
     */
    private static void deleteFromTable(String tableName, int clientId, String trxName) {
        try {
            int count = DB.executeUpdate(
                "DELETE FROM " + tableName + " WHERE AD_Client_ID = ?",
                new Object[]{clientId}, false, trxName);
            if (count > 0) {
                log.fine("已從 " + tableName + " 刪除 " + count + " 筆");
            }
        } catch (Exception e) {
            log.warning("刪除 " + tableName + " 時發生錯誤: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 2: 更新 Activator.java**

```java
package tw.idempiere.sample;

import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import tw.idempiere.sample.cleanup.SampleClientCleanup;
import tw.idempiere.sample.setup.SampleClientSetup;

/**
 * 天地人實業示範資料插件 - OSGi Activator
 *
 * 生命週期：
 * - start(): 檢查並建立示範 Client
 * - stop(): 保留資料（不做任何事）
 * - uninstall: 透過 BundleListener 清理資料
 */
public class Activator implements BundleActivator {

    private static final CLogger log = CLogger.getCLogger(Activator.class);
    private BundleContext context;
    private BundleListener uninstallListener;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        log.info("天地人實業示範資料插件啟動中...");

        // 註冊 uninstall 監聽器
        registerUninstallListener();

        // 初始化示範資料
        try {
            SampleClientSetup.init();
            log.info("天地人實業示範資料插件啟動完成");
        } catch (Exception e) {
            log.log(Level.SEVERE, "初始化失敗", e);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("天地人實業示範資料插件停止（資料保留）");
        // 不做任何事，保留資料
    }

    /**
     * 註冊 uninstall 監聽器
     */
    private void registerUninstallListener() {
        uninstallListener = new BundleListener() {
            @Override
            public void bundleChanged(BundleEvent event) {
                if (event.getBundle() == context.getBundle() &&
                    event.getType() == BundleEvent.UNINSTALLED) {

                    log.info("插件卸載中，開始清理示範資料...");
                    try {
                        SampleClientCleanup.cleanup();
                    } catch (Exception e) {
                        log.log(Level.SEVERE, "清理失敗", e);
                    }
                }
            }
        };
        context.addBundleListener(uninstallListener);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/tw/idempiere/sample/cleanup/SampleClientCleanup.java
git add src/tw/idempiere/sample/Activator.java
git commit -m "feat: 實作 Client 清理功能

- SampleClientCleanup: 依相依性順序刪除所有資料
- Activator: 註冊 uninstall 監聽器觸發清理"
```

---

## Task 7: 建立 README 和最終整合

**Files:**
- Create: `README.md`
- Modify: `src/tw/idempiere/sample/setup/SampleClientSetup.java` (整合呼叫)

- [ ] **Step 1: 更新 SampleClientSetup.java 整合所有設定類別**

確保 `createClient()` 方法正確傳遞參數給各設定類別：

```java
// 在 createClient() 方法中更新以下部分：

// 7. 建立價格表
SamplePriceListSetup plSetup = new SamplePriceListSetup(ctx, trxName, client);
plSetup.createPriceLists();

// 8. 建立 BP
SampleBPSetup bpSetup = new SampleBPSetup(ctx, trxName, client);
bpSetup.createBPartners();

// 9. 建立商品（需要價格表和稅務分類）
SampleProductSetup prodSetup = new SampleProductSetup(ctx, trxName, client);
prodSetup.setSalesPLV(plSetup.getSalesPLV());
prodSetup.setPurchasePLV(plSetup.getPurchasePLV());
prodSetup.setTaxCategoryId(taxSetup.getTaxCategory().getC_TaxCategory_ID());
prodSetup.createProducts();
```

- [ ] **Step 2: 建立 README.md**

```markdown
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

## 示範資料內容

### Client 資訊

| 項目 | 值 |
|------|-----|
| 名稱 | 天地人實業有限公司 |
| Search Key | sample |
| 幣別 | TWD |
| 成本方法 | Average PO |

### 組織架構

- 台北總公司（含台北主倉）
- 台中分公司（含台中倉）
- 高雄倉庫（含高雄倉）

### 主檔資料

- **商品**：30 項（庫存品、服務、BOM 組合品）
- **Business Partner**：15 筆（供應商、客戶、員工）
- **會計科目**：台灣商業會計處理準則（精簡版）

### 登入資訊

| 帳號 | 密碼 | 角色 |
|------|------|------|
| admin | admin | 天地人管理員 |

## 重置資料

卸載插件即可清理所有示範資料：

```
uninstall <bundle-id>
```

重新安裝插件會建立全新的示範環境。

## 開發資訊

### 專案結構

```
src/tw/idempiere/sample/
├── Activator.java           # OSGi 生命週期
├── setup/                   # 資料建立
│   ├── SampleClientSetup.java
│   ├── SampleOrgSetup.java
│   ├── SampleCalendarSetup.java
│   ├── SampleAccountingSetup.java
│   ├── SampleTaxSetup.java
│   ├── SampleBPSetup.java
│   ├── SampleProductSetup.java
│   └── SamplePriceListSetup.java
├── cleanup/                 # 資料清理
│   └── SampleClientCleanup.java
└── data/                    # 資料定義
    ├── ChartOfAccountsTW.java
    ├── OrganizationData.java
    ├── BPartnerData.java
    └── ProductData.java
```

## 授權

Apache License 2.0

## 貢獻

歡迎提交 Issue 和 Pull Request！
```

- [ ] **Step 3: Commit**

```bash
git add README.md
git add src/tw/idempiere/sample/setup/SampleClientSetup.java
git commit -m "docs: 新增 README 和整合最終程式碼

- README.md: 安裝說明、功能說明、開發資訊
- SampleClientSetup: 整合所有設定類別的參數傳遞"
```

---

## Task 8: 最終驗證

- [ ] **Step 1: 確認所有檔案都已建立**

```bash
find . -name "*.java" | head -20
find . -name "*.xml" | head -10
ls -la META-INF/
```

- [ ] **Step 2: 嘗試編譯（若有 Maven 環境）**

```bash
mvn compile
```

- [ ] **Step 3: 最終 Commit**

```bash
git add -A
git status
git log --oneline
```

---

## 總結

| Task | 內容 | 檔案數 |
|------|------|--------|
| 1 | 專案骨架 | 4 |
| 2 | 資料定義類別 | 4 |
| 3 | Client/Org/Calendar 建立 | 3 |
| 4 | 會計/稅務設定 | 2 |
| 5 | BP/商品/價格表 | 3 |
| 6 | 清理功能 | 2 |
| 7 | README 和整合 | 2 |
| 8 | 最終驗證 | - |

**預計總檔案數**：約 20 個檔案
