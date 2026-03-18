/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.io.File;
import java.util.Properties;

import org.compiere.Adempiere;
import org.compiere.model.MClient;
import org.compiere.model.MCurrency;
import org.compiere.model.MSetup;
import org.compiere.model.MSysConfig;
import org.compiere.model.Query;
import org.compiere.model.MTaxCategory;
import org.compiere.print.PrintUtil;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Trx;

import tw.idempiere.sample.util.SetupLog;

/**
 * Client 建立主流程控制類別
 * <p>
 * 使用 iDempiere 標準的 MSetup 類別來建立示範 Client（天地人實業有限公司）。
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class SampleClientSetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleClientSetup.class);

    /** 同步鎖，防止多執行緒同時初始化 */
    private static final Object INIT_LOCK = new Object();

    /** 是否正在初始化中 */
    private static volatile boolean initializing = false;

    /** Client 的 Search Key（唯一識別碼） */
    public static final String CLIENT_VALUE = "sample";

    /** Client 名稱 */
    public static final String CLIENT_NAME = "天地人實業有限公司";

    /** 組織代碼 */
    private static final String ORG_VALUE = "HQ";

    /** 組織名稱 */
    private static final String ORG_NAME = "台北總公司";

    /** 管理員用戶名稱 */
    private static final String ADMIN_USER_NAME = "SampleAdmin";

    /** 一般用戶名稱 */
    private static final String NORMAL_USER_NAME = "SampleUser";

    /** 私有建構子，防止實例化 */
    private SampleClientSetup() {
        // 工具類別，不需要實例化
    }

    /**
     * 檢查 Sample Client 是否已存在
     *
     * @return true=存在, false=不存在
     */
    public static boolean clientExists() {
        return getExistingClient() != null;
    }

    /**
     * 取得現有的 Sample Client
     *
     * @return MClient 物件，不存在時回傳 null
     */
    public static MClient getExistingClient() {
        Properties ctx = Env.getCtx();
        return new Query(ctx, MClient.Table_Name, "Name=?", null)
                .setParameters(CLIENT_NAME)
                .setOnlyActiveRecords(true)
                .first();
    }

    /**
     * 執行完整的初始化流程
     * <p>
     * 使用 iDempiere 標準的 MSetup 類別來建立 Client。
     * </p>
     *
     * @return true=成功（包含已存在的情況）, false=失敗
     */
    public static boolean init() {
        log.info("開始初始化 Sample Client（使用 MSetup）...");

        // 最早的日誌記錄點
        SetupLog.log("init()開始", "SampleClientSetup.init() 被調用");

        // 使用同步鎖防止多執行緒同時初始化
        synchronized (INIT_LOCK) {
            if (initializing) {
                log.warning("初始化正在進行中，跳過此次請求");
                SetupLog.log("init()跳過", "正在初始化中");
                return false;
            }
            initializing = true;
        }

        SetupLog.log("init()繼續", "取得初始化鎖");

        Properties ctx = Env.getCtx();

        // 保存原始 context 值
        int originalClientId = Env.getAD_Client_ID(ctx);
        int originalOrgId = Env.getAD_Org_ID(ctx);
        int originalUserId = Env.getAD_User_ID(ctx);
        int originalRoleId = Env.getAD_Role_ID(ctx);

        try {
            // 設定 System context（使用 System 身份建立新 Client）
            Env.setContext(ctx, Env.AD_CLIENT_ID, 0);
            Env.setContext(ctx, Env.AD_ORG_ID, 0);
            Env.setContext(ctx, Env.AD_USER_ID, 0);  // SuperUser
            Env.setContext(ctx, Env.AD_ROLE_ID, 0);  // System Administrator
            log.info("已設定 System context");

            // 檢查是否已存在
            SetupLog.log("檢查存在", "檢查 Client 是否已存在...");
            if (clientExists()) {
                log.info("Sample Client 已存在，檢查是否需要補建示範資料...");
                MClient existingClient = getExistingClient();
                SetupLog.log("已存在", "Client 已存在: ID=" + (existingClient != null ? existingClient.getAD_Client_ID() : "null"));
                if (existingClient != null) {
                    // 嘗試補建示範資料（如果之前沒建立成功）
                    createSampleDataIfNeeded(ctx, existingClient.getAD_Client_ID());
                }
                return true;
            }
            SetupLog.log("不存在", "Client 不存在，準備建立新 Client");

            // 不再檢查用戶名，只檢查 Client 是否存在（上面已檢查過）

            // 取得必要的參數
            int currencyId = getCurrencyId("TWD"); // 新台幣
            if (currencyId <= 0) {
                currencyId = getCurrencyId("USD"); // 備用：美元
            }
            if (currencyId <= 0) {
                log.severe("找不到貨幣資料");
                return false;
            }

            int countryId = getCountryId("TW"); // 台灣
            if (countryId <= 0) {
                countryId = getCountryId("US"); // 備用：美國
            }
            if (countryId <= 0) {
                log.severe("找不到國家資料");
                return false;
            }

            // 取得會計科目表檔案路徑
            String coaFile = getCoAFilePath();
            if (coaFile == null) {
                log.severe("找不到會計科目表檔案");
                return false;
            }

            log.info("使用參數: Currency=" + currencyId + ", Country=" + countryId + ", CoA=" + coaFile);
            SetupLog.log("參數", "Currency=" + currencyId + ", Country=" + countryId + ", CoA=" + coaFile);

            // 使用 MSetup 建立 Client
            MSetup ms = new MSetup(ctx, 9999);  // WindowNo = 9999

            try {
                // 步驟 1：建立 Client
                SetupLog.log("MSetup步驟1", "建立 Client...");
                if (!ms.createClient(CLIENT_NAME, ORG_VALUE, ORG_NAME,
                        ADMIN_USER_NAME, NORMAL_USER_NAME,
                        null, null, null, null, null,  // Phone, Phone2, Fax, EMail, TaxID
                        null, null,  // AdminUserEmail, NormalUserEmail
                        true)) {  // IsSetInitialPassword
                    SetupLog.log("MSetup步驟1", "失敗: " + ms.getInfo());
                    ms.rollback();
                    return false;
                }
                SetupLog.log("MSetup步驟1", "成功: " + ms.getInfo());

                // 步驟 2：建立會計架構
                SetupLog.log("MSetup步驟2", "建立會計架構...");
                MCurrency currency = MCurrency.get(ctx, currencyId);
                KeyNamePair currencyKP = new KeyNamePair(currencyId, currency.getDescription());

                if (!ms.createAccounting(currencyKP,
                        true,   // IsUseProductDimension
                        true,   // IsUseBPDimension
                        false,  // IsUseProjectDimension
                        false,  // IsUseCampaignDimension
                        false,  // IsUseSalesRegionDimension
                        false,  // IsUseActivityDimension
                        new File(coaFile),
                        true,   // UseDefaultCoA
                        false)) { // InactivateDefaults
                    SetupLog.log("MSetup步驟2", "失敗: " + ms.getInfo());
                    ms.rollback();
                    return false;
                }
                SetupLog.log("MSetup步驟2", "成功: " + ms.getInfo());

                // 步驟 3：建立實體（地址等）
                SetupLog.log("MSetup步驟3", "建立實體...");
                if (!ms.createEntities(countryId,
                        "台北市",  // CityName
                        0,        // C_Region_ID
                        currencyId,
                        "100",    // Postal
                        "中正區")) {  // Address1
                    SetupLog.log("MSetup步驟3", "失敗: " + ms.getInfo());
                    ms.rollback();
                    return false;
                }
                SetupLog.log("MSetup步驟3", "成功: " + ms.getInfo());

                // 步驟 4：建立列印格式
                SetupLog.log("MSetup步驟4", "建立列印格式...");
                PrintUtil.setupPrintForm(ms.getAD_Client_ID(), null);
                SetupLog.log("MSetup步驟4", "完成");

                log.info("基本 Client 建立完成！Client ID = " + ms.getAD_Client_ID());

                // 注意：MSetup 在 createEntities() 完成後會自動提交並關閉其內部交易
                // 所以基礎資料（Client, Org, AcctSchema）已經持久化到資料庫
                SetupLog.log("MSetup完成", "MSetup 已自動提交其交易，基礎資料已持久化");

                // 驗證基礎資料是否真的存在於資料庫
                int verifyClientId = DB.getSQLValue(null,
                    "SELECT AD_Client_ID FROM AD_Client WHERE AD_Client_ID=?", ms.getAD_Client_ID());
                SetupLog.log("驗證Client", "資料庫中 Client ID = " + verifyClientId);

                // 步驟 5：建立示範資料
                log.info("步驟 5：建立示範資料...");
                int newClientId = ms.getAD_Client_ID();

                // 立即寫入日誌（在 try 區塊外）
                SetupLog.log("MSetup完成", "Client ID = " + newClientId + ", MSetup 建立成功，準備建立示範資料");

                // 設定 context 為新建立的 Client（包含完整的使用者資訊）
                Env.setContext(ctx, Env.AD_CLIENT_ID, newClientId);
                Env.setContext(ctx, Env.AD_ORG_ID, 0);
                Env.setContext(ctx, Env.AD_USER_ID, 100);  // SuperUser
                Env.setContext(ctx, Env.AD_ROLE_ID, 0);    // System Administrator

                SetupLog.log("Context設定", "AD_Client_ID=" + newClientId + ", AD_User_ID=100");

                // 使用全新的交易來建立示範資料
                // 確保不會與 MSetup 的交易混淆
                String trxName = Trx.createTrxName("SampleData_" + System.currentTimeMillis());
                Trx trx = Trx.get(trxName, true);
                trx.start(); // 明確啟動交易

                SetupLog.log("交易建立", "交易名稱: " + trxName);

                // 驗證交易狀態
                try {
                    java.sql.Connection conn = trx.getConnection();
                    if (conn != null) {
                        SetupLog.log("交易連接", "autoCommit=" + conn.getAutoCommit() +
                            ", closed=" + conn.isClosed());
                    }
                } catch (Exception connEx) {
                    SetupLog.log("交易連接", "無法取得連接: " + connEx.getMessage());
                }

                try {
                    SetupLog.log("開始", "進入 try 區塊，開始建立示範資料");

                    // 5.0 調整會計架構設定（在交易中執行）
                    SetupLog.log("步驟 5.0", "調整會計架構設定...");
                    configureAccountingSchema(ctx, newClientId, trxName);
                    SetupLog.log("步驟 5.0", "完成");
                    checkTrxStatus(trx, "5.0後");

                    // 5.0.1 開啟會計期間
                    SetupLog.log("步驟 5.0.1", "開啟會計期間...");
                    openAccountingPeriods(ctx, newClientId, trxName);
                    SetupLog.log("步驟 5.0.1", "完成");
                    checkTrxStatus(trx, "5.0.1後");

                    // 5.0.2 建立組織架構（台中分公司、高雄倉庫）和倉庫（台北主倉、台中倉、高雄倉）
                    SetupLog.log("步驟 5.0.2", "建立組織架構和倉庫...");
                    if (!SampleOrgSetup.createOrganizations(ctx, newClientId, trxName)) {
                        SetupLog.log("步驟 5.0.2", "警告：無法建立組織架構");
                    } else {
                        SetupLog.log("步驟 5.0.2", "完成");
                    }
                    checkTrxStatus(trx, "5.0.2後");

                    // 5.1 建立稅務設定
                    SetupLog.log("步驟 5.1", "建立稅務設定...");
                    MTaxCategory taxCategory = SampleTaxSetup.createTax(ctx, newClientId, 0, trxName);
                    if (taxCategory == null) {
                        SetupLog.log("步驟 5.1", "警告：無法建立稅務設定");
                    } else {
                        SetupLog.log("步驟 5.1", "完成");
                    }
                    int taxCategoryId = SampleTaxSetup.getTaxCategoryId();
                    checkTrxStatus(trx, "5.1後");

                    // 5.2 建立價格表
                    SetupLog.log("步驟 5.2", "建立價格表...");
                    if (!SamplePriceListSetup.createPriceLists(ctx, newClientId, trxName)) {
                        SetupLog.log("步驟 5.2", "警告：無法建立價格表");
                    } else {
                        SetupLog.log("步驟 5.2", "完成");
                    }
                    checkTrxStatus(trx, "5.2後");

                    // 5.3 建立商品
                    SetupLog.log("步驟 5.3", "建立商品...");
                    if (!SampleProductSetup.createProducts(ctx, newClientId, taxCategoryId,
                            SamplePriceListSetup.getSalesPLV(),
                            SamplePriceListSetup.getPurchasePLV(),
                            trxName)) {
                        SetupLog.log("步驟 5.3", "警告：無法建立商品");
                    } else {
                        SetupLog.log("步驟 5.3", "完成");
                    }
                    checkTrxStatus(trx, "5.3後");

                    // 5.4 建立業務夥伴
                    SetupLog.log("步驟 5.4", "建立業務夥伴...");
                    if (!SampleBPSetup.createBPartners(ctx, newClientId, trxName)) {
                        SetupLog.log("步驟 5.4", "警告：無法建立業務夥伴");
                    } else {
                        SetupLog.log("步驟 5.4", "完成");
                    }
                    checkTrxStatus(trx, "5.4後");

                    // 5.5 建立台灣會計科目並設定 Defaults
                    SetupLog.log("步驟 5.5", "建立台灣會計科目...");
                    TaiwanCoASetup.createTaiwanCoA(ctx, newClientId, trxName);
                    SetupLog.log("步驟 5.5", "完成");

                    // 提交前驗證：直接使用 JDBC 查詢（不依賴 Trx）
                    SetupLog.log("提交前驗證", "開始...");
                    try {
                        java.sql.Connection conn = trx.getConnection();
                        SetupLog.log("連接狀態", "autoCommit=" + conn.getAutoCommit() +
                            ", closed=" + conn.isClosed() + ", trx.isActive=" + trx.isActive());

                        // 使用同一連接直接查詢
                        try (java.sql.PreparedStatement pstmt = conn.prepareStatement(
                                "SELECT COUNT(*) FROM C_ElementValue WHERE AD_Client_ID=?")) {
                            pstmt.setInt(1, newClientId);
                            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                                if (rs.next()) {
                                    int count = rs.getInt(1);
                                    SetupLog.log("提交前驗證", "交易內 C_ElementValue 數量: " + count);
                                }
                            }
                        }

                        // 查詢台灣會計科目是否存在
                        try (java.sql.PreparedStatement pstmt = conn.prepareStatement(
                                "SELECT COUNT(*) FROM C_ElementValue WHERE AD_Client_ID=? AND Value IN ('1','11','111')")) {
                            pstmt.setInt(1, newClientId);
                            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                                if (rs.next()) {
                                    int twCount = rs.getInt(1);
                                    SetupLog.log("提交前驗證", "台灣會科數量: " + twCount);
                                }
                            }
                        }

                        // 直接使用 Connection.commit() 提交
                        SetupLog.log("提交", "使用 Connection.commit() 直接提交...");
                        conn.commit();
                        SetupLog.log("提交結果", "Connection.commit() 完成");

                    } catch (Exception connEx) {
                        SetupLog.logError("連接錯誤", "提交過程發生錯誤", connEx);
                    }

                    // ========== 提交後驗證：使用新連接查詢所有重要資料 ==========
                    SetupLog.log("驗證開始", "=== 開始提交後驗證（無交易） ===");

                    // 1. 組織驗證
                    int orgCount = DB.getSQLValue(null,
                        "SELECT COUNT(*) FROM AD_Org WHERE AD_Client_ID=? AND AD_Org_ID > 0", newClientId);
                    SetupLog.log("驗證-組織", "AD_Org 數量: " + orgCount + " (預期: 3)");

                    // 2. 倉庫驗證
                    int whCount = DB.getSQLValue(null,
                        "SELECT COUNT(*) FROM M_Warehouse WHERE AD_Client_ID=?", newClientId);
                    SetupLog.log("驗證-倉庫", "M_Warehouse 數量: " + whCount + " (預期: 3)");

                    // 3. 會計架構驗證
                    int asCount = DB.getSQLValue(null,
                        "SELECT COUNT(*) FROM C_AcctSchema WHERE AD_Client_ID=?", newClientId);
                    SetupLog.log("驗證-會計架構", "C_AcctSchema 數量: " + asCount + " (預期: 1)");

                    // 4. 會計科目驗證（總數和台灣會科）
                    int evTotal = DB.getSQLValue(null,
                        "SELECT COUNT(*) FROM C_ElementValue WHERE AD_Client_ID=?", newClientId);
                    int evTaiwan = DB.getSQLValue(null,
                        "SELECT COUNT(*) FROM C_ElementValue WHERE AD_Client_ID=? AND Value ~ '^[1-8]'", newClientId);
                    SetupLog.log("驗證-會計科目", "C_ElementValue 總數: " + evTotal + ", 台灣會科(1-8開頭): " + evTaiwan + " (預期: 111)");

                    // 5. ValidCombination 驗證
                    int vcCount = DB.getSQLValue(null,
                        "SELECT COUNT(*) FROM C_ValidCombination WHERE AD_Client_ID=?", newClientId);
                    SetupLog.log("驗證-VC", "C_ValidCombination 數量: " + vcCount);

                    // 6. 稅務驗證
                    int taxCount = DB.getSQLValue(null,
                        "SELECT COUNT(*) FROM C_Tax WHERE AD_Client_ID=?", newClientId);
                    SetupLog.log("驗證-稅務", "C_Tax 數量: " + taxCount);

                    // 7. 價格表驗證
                    int plCount = DB.getSQLValue(null,
                        "SELECT COUNT(*) FROM M_PriceList WHERE AD_Client_ID=?", newClientId);
                    SetupLog.log("驗證-價格表", "M_PriceList 數量: " + plCount);

                    // 8. 商品驗證
                    int prodCount = DB.getSQLValue(null,
                        "SELECT COUNT(*) FROM M_Product WHERE AD_Client_ID=?", newClientId);
                    SetupLog.log("驗證-商品", "M_Product 數量: " + prodCount);

                    // 9. 業務夥伴驗證
                    int bpCount = DB.getSQLValue(null,
                        "SELECT COUNT(*) FROM C_BPartner WHERE AD_Client_ID=?", newClientId);
                    SetupLog.log("驗證-業務夥伴", "C_BPartner 數量: " + bpCount);

                    // 總結
                    boolean success = (orgCount >= 3 && whCount >= 3 && evTaiwan >= 100);
                    if (success) {
                        SetupLog.log("驗證完成", "=== 示範資料建立成功！核心資料已持久化 ===");
                    } else {
                        SetupLog.log("驗證失敗", "=== 部分資料未持久化！請檢查日誌 ===");
                    }

                } catch (Exception dataEx) {
                    SetupLog.logError("錯誤", "建立示範資料時發生錯誤", dataEx);
                    log.severe("建立示範資料時發生錯誤: " + dataEx.getMessage());
                    dataEx.printStackTrace();
                    trx.rollback();
                    // 不回傳 false，因為基本 Client 已經建立成功
                } finally {
                    trx.close();
                }

                log.info("Sample Client 初始化完成！Client ID = " + newClientId);
                return true;

            } catch (Exception e) {
                log.severe("初始化時發生錯誤: " + e.getMessage());
                e.printStackTrace();
                ms.rollback();
                return false;
            }

        } finally {
            // 恢復原始 context
            Env.setContext(ctx, Env.AD_CLIENT_ID, originalClientId);
            Env.setContext(ctx, Env.AD_ORG_ID, originalOrgId);
            Env.setContext(ctx, Env.AD_USER_ID, originalUserId);
            Env.setContext(ctx, Env.AD_ROLE_ID, originalRoleId);
            log.info("已恢復原始 context");

            // 重置初始化標記
            synchronized (INIT_LOCK) {
                initializing = false;
            }
        }
    }

    /**
     * 取得貨幣 ID
     *
     * @param isoCode ISO 貨幣代碼（如 TWD, USD）
     * @return 貨幣 ID，找不到時回傳 0
     */
    private static int getCurrencyId(String isoCode) {
        String sql = "SELECT C_Currency_ID FROM C_Currency WHERE ISO_Code = ?";
        return DB.getSQLValue(null, sql, isoCode);
    }

    /**
     * 取得國家 ID
     *
     * @param countryCode 國家代碼（如 TW, US）
     * @return 國家 ID，找不到時回傳 0
     */
    private static int getCountryId(String countryCode) {
        String sql = "SELECT C_Country_ID FROM C_Country WHERE CountryCode = ?";
        return DB.getSQLValue(null, sql, countryCode);
    }

    /**
     * 取得會計科目表檔案路徑
     * <p>
     * 優先使用台灣會計科目表（AccountingTW.csv），
     * 若找不到則使用系統預設的會計科目表。
     * </p>
     *
     * @return 檔案路徑，找不到時回傳 null
     */
    private static String getCoAFilePath() {
        // 使用系統預設 CoA，建立完成後再更新為台灣會計科目
        // 嘗試從系統設定取得
        String coaPath = MSysConfig.getValue(MSysConfig.DEFAULT_COA_PATH, null, 0);

        if (coaPath == null || coaPath.isEmpty()) {
            // 使用預設路徑
            coaPath = Adempiere.getAdempiereHome() + File.separator + "data"
                    + File.separator + "import"
                    + File.separator + "AccountingDefaultsOnly.csv";
        }

        File coaFile = new File(coaPath);
        if (coaFile.exists() && coaFile.canRead()) {
            return coaPath;
        }

        // 嘗試其他常見路徑
        String[] alternativePaths = {
            "/opt/idempiere/data/import/AccountingDefaultsOnly.csv",
            "C:\\idempiere-server\\data\\import\\AccountingDefaultsOnly.csv",
            System.getProperty("user.home") + "/idempiere/data/import/AccountingDefaultsOnly.csv"
        };

        for (String path : alternativePaths) {
            File file = new File(path);
            if (file.exists() && file.canRead()) {
                return path;
            }
        }

        log.warning("找不到會計科目表檔案，嘗試的路徑: " + coaPath);
        return coaPath; // 返回預設路徑，讓 MSetup 處理錯誤
    }

    /**
     * 從 Bundle 資源中提取台灣會計科目表到暫存檔案
     *
     * @return 暫存檔案路徑，失敗時回傳 null
     */
    private static String extractTaiwanCoA() {
        try {
            System.out.println("[TW-CoA] 嘗試讀取台灣會計科目表...");

            // 從 ClassLoader 讀取資源
            java.io.InputStream is = SampleClientSetup.class.getClassLoader()
                    .getResourceAsStream("data/AccountingTW.csv");

            if (is == null) {
                System.out.println("[TW-CoA] ClassLoader 找不到 data/AccountingTW.csv，嘗試其他路徑...");
                // 嘗試其他路徑
                is = SampleClientSetup.class.getResourceAsStream("/data/AccountingTW.csv");
            }

            if (is == null) {
                System.out.println("[TW-CoA] 也找不到 /data/AccountingTW.csv，放棄使用台灣會計科目");
                log.warning("找不到 Bundle 內的台灣會計科目表");
                return null;
            }

            System.out.println("[TW-CoA] 找到資源，開始提取...");

            // 建立暫存檔案
            File tempFile = File.createTempFile("AccountingTW_", ".csv");
            tempFile.deleteOnExit();

            // 複製內容到暫存檔案
            int totalBytes = 0;
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                 java.io.BufferedInputStream bis = new java.io.BufferedInputStream(is)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
            }

            System.out.println("[TW-CoA] 已提取 " + totalBytes + " bytes 到: " + tempFile.getAbsolutePath());
            log.info("已提取台灣會計科目表到: " + tempFile.getAbsolutePath() + " (" + totalBytes + " bytes)");
            return tempFile.getAbsolutePath();

        } catch (Exception e) {
            System.out.println("[TW-CoA] 錯誤: " + e.getMessage());
            e.printStackTrace();
            log.warning("提取台灣會計科目表時發生錯誤: " + e.getMessage());
            return null;
        }
    }

    /**
     * 取得 Client ID
     *
     * @return Client ID，不存在時回傳 -1
     */
    public static int getClientId() {
        MClient client = getExistingClient();
        return client != null ? client.getAD_Client_ID() : -1;
    }

    /**
     * 檢查並補建示範資料（如果尚未建立）
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     */
    private static void createSampleDataIfNeeded(Properties ctx, int clientId) {
        log.info("檢查 Client " + clientId + " 的示範資料...");
        SetupLog.log("補建開始", "createSampleDataIfNeeded 被調用，Client ID=" + clientId);

        // 設定 context 為該 Client
        Env.setContext(ctx, Env.AD_CLIENT_ID, clientId);
        Env.setContext(ctx, Env.AD_ORG_ID, 0);

        // 使用交易來處理資料
        String trxName = Trx.createTrxName("SampleDataFix_" + System.currentTimeMillis());
        Trx trx = Trx.get(trxName, true);
        trx.start(); // 明確啟動交易
        SetupLog.log("補建交易", "交易名稱: " + trxName);

        try {
            // 1. 調整會計架構設定（CostingMethod, AutoPeriodControl）
            log.info("補建：調整會計架構設定...");
            configureAccountingSchema(ctx, clientId, trxName);

            // 2. 開啟會計期間
            log.info("補建：開啟會計期間...");
            openAccountingPeriods(ctx, clientId, trxName);

            // 3. 建立組織架構和倉庫
            log.info("補建：建立組織架構和倉庫...");
            SampleOrgSetup.createOrganizations(ctx, clientId, trxName);

            // 4. 建立台灣會計科目
            log.info("補建：建立台灣會計科目...");
            TaiwanCoASetup.createTaiwanCoA(ctx, clientId, trxName);

            // 檢查是否已有商品類別（作為示範資料是否已建立的指標）
            // 新版資料使用 PAPER, WRITING, OFFICE 等代碼
            int productCategoryCount = DB.getSQLValue(trxName,
                    "SELECT COUNT(*) FROM M_Product_Category WHERE AD_Client_ID=? AND Value IN ('PAPER', 'WRITING', 'OFFICE', 'FILE', 'SERVICE', 'GENERAL')",
                    clientId);

            if (productCategoryCount > 0) {
                log.info("商品類別已存在，跳過商品和業務夥伴建立");
                // 提交前驗證
                int preCount = DB.getSQLValue(trxName,
                    "SELECT COUNT(*) FROM C_ElementValue WHERE AD_Client_ID=? AND Value IN ('1','11','111')", clientId);
                SetupLog.log("補建提交前", "台灣會科數量(交易內): " + preCount);

                boolean committed = trx.commit();
                SetupLog.log("補建提交", "commit結果: " + committed);

                // 提交後驗證（使用 null 表示無交易）
                int postCount = DB.getSQLValue(null,
                    "SELECT COUNT(*) FROM C_ElementValue WHERE AD_Client_ID=? AND Value IN ('1','11','111')", clientId);
                SetupLog.log("補建提交後", "台灣會科數量(資料庫): " + postCount);

                if (postCount == 0 && preCount > 0) {
                    SetupLog.log("補建異常", "數據在提交後消失！");
                }
                return;
            }

            log.info("未找到商品類別，開始建立商品和業務夥伴...");

            // 建立稅務設定
            log.info("補建稅務設定...");
            MTaxCategory taxCategory = SampleTaxSetup.createTax(ctx, clientId, 0, trxName);
            int taxCategoryId = taxCategory != null ? taxCategory.getC_TaxCategory_ID() : 0;

            // 建立價格表
            log.info("補建價格表...");
            SamplePriceListSetup.createPriceLists(ctx, clientId, trxName);

            // 建立商品
            log.info("補建商品...");
            SampleProductSetup.createProducts(ctx, clientId, taxCategoryId,
                    SamplePriceListSetup.getSalesPLV(),
                    SamplePriceListSetup.getPurchasePLV(),
                    trxName);

            // 建立業務夥伴
            log.info("補建業務夥伴...");
            SampleBPSetup.createBPartners(ctx, clientId, trxName);

            trx.commit();
            log.info("示範資料補建完成！");

        } catch (Exception e) {
            log.warning("補建示範資料時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            trx.rollback();
        } finally {
            trx.close();
        }
    }

    /**
     * 調整會計架構設定
     * <p>
     * 設定：
     * <ul>
     *   <li>Costing Method = Average PO</li>
     *   <li>Auto Period Control = N（關閉）</li>
     * </ul>
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     */
    private static void configureAccountingSchema(Properties ctx, int clientId, String trxName) {
        try {
            // 查詢此 Client 的 Accounting Schema
            int acctSchemaId = DB.getSQLValue(trxName,
                "SELECT C_AcctSchema_ID FROM C_AcctSchema WHERE AD_Client_ID = ?", clientId);

            if (acctSchemaId > 0) {
                org.compiere.model.MAcctSchema as = new org.compiere.model.MAcctSchema(ctx, acctSchemaId, trxName);

                // 設定 Costing Method = Average PO
                as.setCostingMethod(org.compiere.model.MAcctSchema.COSTINGMETHOD_AveragePO);

                // 關閉 Auto Period Control
                as.setAutoPeriodControl(false);

                if (as.save()) {
                    log.info("會計架構設定完成：CostingMethod=AveragePO, AutoPeriodControl=N");
                } else {
                    log.warning("無法儲存會計架構設定");
                }
            } else {
                log.warning("找不到會計架構");
            }
        } catch (Exception e) {
            log.warning("調整會計架構設定時發生錯誤: " + e.getMessage());
        }
    }

    /**
     * 開啟會計期間
     * <p>
     * 將當年度的所有期間狀態設為 Open。
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     */
    private static void openAccountingPeriods(Properties ctx, int clientId, String trxName) {
        try {
            // 查詢此 Client 的所有 Period Control 並設為 Open
            String sql = "UPDATE C_PeriodControl SET PeriodStatus = 'O' " +
                "WHERE C_Period_ID IN (SELECT C_Period_ID FROM C_Period WHERE AD_Client_ID = ?) " +
                "AND PeriodStatus != 'P'"; // 不更動永久關閉的期間

            int updated = DB.executeUpdate(sql, new Object[]{clientId}, false, trxName);
            log.info("已開啟 " + updated + " 個會計期間控制記錄");

            // 也更新 Period 本身的 PeriodType（確保是標準期間）
            sql = "UPDATE C_Period SET PeriodType = 'S' WHERE AD_Client_ID = ? AND PeriodType IS NULL";
            DB.executeUpdate(sql, new Object[]{clientId}, false, trxName);

        } catch (Exception e) {
            log.warning("開啟會計期間時發生錯誤: " + e.getMessage());
        }
    }

    /**
     * 更新會計科目名稱為台灣中文
     * <p>
     * 將 Default Accounts 的英文名稱更新為台灣中文名稱。
     * 同時更新 C_ValidCombination.Description 以便在 Accounting Schema Defaults 視窗正確顯示。
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     */
    private static void updateAccountNamesToChinese(Properties ctx, int clientId, String trxName) {
        log.info("更新會計科目為台灣中文名稱...");

        // 台灣會計科目名稱對照表
        String[][] accountNames = {
            {"B_ASSET", "銀行存款"},
            {"B_INTRANSIT", "在途資金"},
            {"B_UNALLOCATEDCASH", "未分配收款"},
            {"B_PAYMENTSELECT", "付款選擇"},
            {"B_INTERESTREV", "利息收入"},
            {"B_INTERESTEXP", "利息費用"},
            {"CB_ASSET", "零用金"},
            {"CB_CASHTRANSFER", "零用金在途"},
            {"CB_DIFFERENCES", "零用金差異"},
            {"CB_EXPENSE", "零用金費用"},
            {"CB_RECEIPT", "零用金收入"},
            {"C_RECEIVABLE", "應收帳款"},
            {"C_RECEIVABLE_SERVICES", "應收服務款"},
            {"C_PREPAYMENT", "客戶預收款"},
            {"V_LIABILITY", "應付帳款"},
            {"V_LIABILITY_SERVICES", "應付服務款"},
            {"V_PREPAYMENT", "供應商預付款"},
            {"P_REVENUE", "銷貨收入"},
            {"P_EXPENSE", "商品費用"},
            {"P_ASSET", "商品存貨"},
            {"P_COGS", "銷貨成本"},
            {"P_PURCHASEPRICEVARIANCE", "進貨價差"},
            {"P_INVOICEPRICEVARIANCE", "發票價差"},
            {"P_TRADEDISCOUNTREC", "進貨折扣收入"},
            {"P_TRADEDISCOUNTGRANT", "銷貨折讓"},
            {"P_COSTOFPRODUCTION", "生產成本"},
            {"P_LABOR", "人工成本"},
            {"P_BURDEN", "製造費用"},
            {"P_OUTSIDEPROCESSING", "委外加工"},
            {"P_OVERHEAD", "間接費用"},
            {"P_SCRAP", "報廢損失"},
            {"P_WIP", "在製品"},
            {"P_METHODCHANGEVARIANCE", "方法變更差異"},
            {"P_USAGEVARIANCE", "用量差異"},
            {"P_RATEVARIANCE", "費率差異"},
            {"P_MIXVARIANCE", "組合差異"},
            {"P_FLOORSTOCK", "車間物料"},
            {"P_COSTADJUSTMENT", "成本調整"},
            {"P_INVENTORYCLEARING", "存貨清算"},
            {"P_FREIGHT", "運費"},
            {"T_DUE", "應付稅額"},
            {"T_CREDIT", "進項稅額"},
            {"T_EXPENSE", "稅務費用"},
            {"W_DIFFERENCES", "倉庫差異"},
            {"W_INVACTUALADJUST", "存貨實際調整"},
            {"W_REVALUATION", "存貨重估"},
            {"PJ_ASSET", "專案資產"},
            {"PJ_WIP", "專案在製品"},
            {"CH_EXPENSE", "雜項費用"},
            {"CH_REVENUE", "雜項收入"},
            {"UNREALIZEDGAIN", "未實現匯兌利益"},
            {"UNREALIZEDLOSS", "未實現匯兌損失"},
            {"REALIZEDGAIN", "已實現匯兌利益"},
            {"REALIZEDLOSS", "已實現匯兌損失"},
            {"CURRENCYBALANCING", "匯率差額"},
            {"DEFAULT", "預設帳戶"},
            {"SUSPENSEBALANCING", "暫記調整"},
            {"RETAINEDEARNING", "保留盈餘"},
            {"INCOMESUMMARY", "本期損益"},
            {"COMMITMENTOFFSET", "採購承諾"},
            {"COMMITMENTOFFSETSALES", "銷售承諾"},
            {"DEFAULTS_NOT_CONFIGURED", "未設定預設帳戶"}
        };

        try {
            int updated = 0;
            for (String[] mapping : accountNames) {
                String value = mapping[0];
                String chineseName = mapping[1];

                // 更新 C_ElementValue.Name
                String sql = "UPDATE C_ElementValue SET Name = ? WHERE AD_Client_ID = ? AND Value = ?";
                int result = DB.executeUpdate(sql, new Object[]{chineseName, clientId, value}, false, trxName);
                if (result > 0) {
                    updated++;
                }
            }
            log.info("已更新 " + updated + " 個會計科目為台灣中文名稱");

            // 更新 C_ValidCombination.Description
            // 這個欄位是 Accounting Schema Defaults 視窗實際顯示的內容
            updateValidCombinationDescriptions(clientId, trxName);

        } catch (Exception e) {
            log.warning("更新會計科目名稱時發生錯誤: " + e.getMessage());
        }
    }

    /**
     * 更新 C_ValidCombination.Description 以顯示中文帳戶名稱
     * <p>
     * Accounting Schema Defaults 視窗中顯示的是 C_ValidCombination.Description，
     * 而非直接從 C_ElementValue.Name 取得。此方法將 Description 更新為包含中文名稱。
     * </p>
     *
     * @param clientId Client ID
     * @param trxName 交易名稱
     */
    private static void updateValidCombinationDescriptions(int clientId, String trxName) {
        log.info("更新 C_ValidCombination.Description 為中文...");

        try {
            // 透過 JOIN 取得會計科目名稱並更新 Description
            // Description 格式通常是帳戶代碼-名稱的組合
            String sql = "UPDATE C_ValidCombination vc SET Description = " +
                "(SELECT ev.Value || '-' || ev.Name FROM C_ElementValue ev " +
                " WHERE ev.C_ElementValue_ID = vc.Account_ID) " +
                "WHERE vc.AD_Client_ID = ? AND vc.Account_ID IS NOT NULL";

            int updated = DB.executeUpdate(sql, new Object[]{clientId}, false, trxName);
            log.info("已更新 " + updated + " 個 ValidCombination 的 Description");

            // 同時更新 Alias（某些視圖可能使用 Alias）
            sql = "UPDATE C_ValidCombination vc SET Alias = " +
                "(SELECT ev.Name FROM C_ElementValue ev " +
                " WHERE ev.C_ElementValue_ID = vc.Account_ID) " +
                "WHERE vc.AD_Client_ID = ? AND vc.Account_ID IS NOT NULL";

            updated = DB.executeUpdate(sql, new Object[]{clientId}, false, trxName);
            log.info("已更新 " + updated + " 個 ValidCombination 的 Alias");

        } catch (Exception e) {
            log.warning("更新 ValidCombination Description 時發生錯誤: " + e.getMessage());
        }
    }

    /**
     * 檢查交易狀態，用於診斷 "transaction aborted" 問題
     *
     * @param trx 交易物件
     * @param step 步驟名稱
     */
    private static void checkTrxStatus(Trx trx, String step) {
        try {
            java.sql.Connection conn = trx.getConnection();
            if (conn != null) {
                // 嘗試執行簡單查詢來檢查交易是否正常
                try (java.sql.Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT 1");
                    SetupLog.log("交易檢查", step + ": OK");
                }
            }
        } catch (java.sql.SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("aborted")) {
                SetupLog.log("交易異常", step + ": 交易已 ABORTED! " + msg);
            } else {
                SetupLog.log("交易錯誤", step + ": " + msg);
            }
        } catch (Exception e) {
            SetupLog.log("交易檢查失敗", step + ": " + e.getMessage());
        }
    }
}
