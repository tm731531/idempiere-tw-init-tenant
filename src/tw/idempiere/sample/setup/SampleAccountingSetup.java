/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaDefault;
import org.compiere.model.MAccount;
import org.compiere.model.MElement;
import org.compiere.model.MElementValue;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import tw.idempiere.sample.data.ChartOfAccountsTW;

/**
 * 會計架構建立
 * <p>
 * 負責建立示範公司的會計架構，包含：
 * <ul>
 *   <li>Element（科目表）</li>
 *   <li>ElementValue（各會計科目）</li>
 *   <li>Accounting Schema（會計架構）</li>
 *   <li>Default Accounts（預設科目設定）</li>
 * </ul>
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class SampleAccountingSetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleAccountingSetup.class);

    /** 會計架構名稱 */
    private static final String ACCT_SCHEMA_NAME = "天地人會計架構";

    /** 科目表名稱 */
    private static final String ELEMENT_NAME = "天地人會計科目表";

    /** 科目代碼對應 ElementValue ID 的對照表 */
    private static Map<String, Integer> elementValueIdMap = new HashMap<>();

    /** 建立的會計架構 ID */
    private static int acctSchemaId = 0;

    /** 建立的 Element ID */
    private static int elementId = 0;

    /** 私有建構子，防止實例化 */
    private SampleAccountingSetup() {
        // 工具類別，不需要實例化
    }

    /**
     * 建立完整的會計架構
     * <p>
     * 建立流程：
     * <ol>
     *   <li>建立 Element（科目表）</li>
     *   <li>建立所有 ElementValue（會計科目）</li>
     *   <li>建立 Accounting Schema（會計架構）</li>
     *   <li>設定 Default Accounts（預設科目）</li>
     * </ol>
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param orgId 組織 ID
     * @param trxName 交易名稱
     * @return 建立的 MAcctSchema，失敗時回傳 null
     */
    public static MAcctSchema createAccountingSchema(Properties ctx, int clientId, int orgId, String trxName) {
        log.info("開始建立會計架構...");

        // 清空對照表
        elementValueIdMap.clear();
        acctSchemaId = 0;
        elementId = 0;

        try {
            // 步驟 1：建立 Element（科目表）
            MElement element = createElement(ctx, clientId, trxName);
            if (element == null) {
                log.severe("無法建立科目表");
                return null;
            }
            elementId = element.getC_Element_ID();

            // 步驟 2：建立所有 ElementValue（會計科目）
            if (!createAccounts(ctx, clientId, element, trxName)) {
                log.severe("無法建立會計科目");
                return null;
            }

            // 步驟 3：建立 Accounting Schema（會計架構）
            MAcctSchema acctSchema = createAcctSchema(ctx, clientId, element, trxName);
            if (acctSchema == null) {
                log.severe("無法建立會計架構");
                return null;
            }
            acctSchemaId = acctSchema.getC_AcctSchema_ID();

            // 步驟 4：設定 Default Accounts（預設科目）
            if (!setupDefaultAccounts(ctx, clientId, acctSchema, trxName)) {
                log.severe("無法設定預設科目");
                return null;
            }

            log.info("會計架構建立完成: " + ACCT_SCHEMA_NAME);
            return acctSchema;

        } catch (Exception e) {
            log.severe("建立會計架構時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 建立 Element（科目表）
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return 建立的 MElement，失敗時回傳 null
     */
    private static MElement createElement(Properties ctx, int clientId, String trxName) {
        log.info("建立科目表: " + ELEMENT_NAME);

        MElement element = new MElement(ctx, 0, trxName);
        element.setAD_Org_ID(0); // 科目表設定在 Client 層級
        element.setName(ELEMENT_NAME);
        element.setDescription("天地人實業有限公司會計科目表，依據台灣商業會計法規定");
        element.setElementType(MElement.ELEMENTTYPE_Account); // 設定為科目類型

        if (!element.save()) {
            log.severe("無法儲存科目表");
            return null;
        }

        log.info("已建立科目表: " + ELEMENT_NAME + " (ID=" + element.getC_Element_ID() + ")");
        return element;
    }

    /**
     * 建立所有會計科目（ElementValue）
     * <p>
     * 使用 ChartOfAccountsTW.ACCOUNTS 定義的科目資料建立所有會計科目。
     * 需要先處理階層關係（父子科目的關聯）。
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param element 科目表
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    private static boolean createAccounts(Properties ctx, int clientId, MElement element, String trxName) {
        log.info("建立會計科目...");

        // 第一次遍歷：建立所有科目（暫不設定父科目）
        for (String[] acctData : ChartOfAccountsTW.ACCOUNTS) {
            String value = acctData[ChartOfAccountsTW.ACCT_VALUE];
            String name = acctData[ChartOfAccountsTW.ACCT_NAME];
            String type = acctData[ChartOfAccountsTW.ACCT_TYPE];
            boolean isSummary = "Y".equals(acctData[ChartOfAccountsTW.ACCT_IS_SUMMARY]);

            MElementValue elementValue = new MElementValue(ctx, 0, trxName);
            elementValue.setAD_Org_ID(0); // 科目設定在 Client 層級
            elementValue.setC_Element_ID(element.getC_Element_ID());
            elementValue.setValue(value);
            elementValue.setName(name);
            elementValue.setAccountType(type);
            elementValue.setIsSummary(isSummary);
            elementValue.setPostActual(true); // 可過帳實際金額
            elementValue.setPostBudget(true); // 可過帳預算金額
            elementValue.setPostEncumbrance(false); // 不需要承諾金額
            elementValue.setPostStatistical(false); // 不需要統計金額

            if (!elementValue.save()) {
                log.severe("無法建立科目: " + name);
                return false;
            }

            // 記錄科目 ID
            elementValueIdMap.put(value, elementValue.getC_ElementValue_ID());
            log.fine("已建立科目: " + value + " - " + name);
        }

        // 注意：父科目關聯設定暫時跳過（某些版本可能不支援 setParent_ID）
        // 如需設定科目階層，請透過 iDempiere 介面手動設定

        log.info("已建立 " + elementValueIdMap.size() + " 個會計科目");
        return true;
    }

    /**
     * 建立 Accounting Schema（會計架構）
     * <p>
     * 會計架構設定：
     * <ul>
     *   <li>成本法：Average PO（加權平均-採購）</li>
     *   <li>成本層級：Client（公司層級）</li>
     *   <li>自動帳期控制：false（手動控制）</li>
     *   <li>幣別：TWD（新台幣）</li>
     * </ul>
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param element 科目表
     * @param trxName 交易名稱
     * @return 建立的 MAcctSchema，失敗時回傳 null
     */
    private static MAcctSchema createAcctSchema(Properties ctx, int clientId, MElement element, String trxName) {
        log.info("建立會計架構: " + ACCT_SCHEMA_NAME);

        // 查詢 TWD 幣別 ID
        int currencyId = DB.getSQLValue(trxName,
            "SELECT C_Currency_ID FROM C_Currency WHERE ISO_Code=?", "TWD");
        if (currencyId <= 0) {
            // 如果找不到 TWD，使用 USD 作為備用
            currencyId = DB.getSQLValue(trxName,
                "SELECT C_Currency_ID FROM C_Currency WHERE ISO_Code=?", "USD");
            log.warning("找不到 TWD 幣別，使用 USD 作為備用");
        }

        MAcctSchema acctSchema = new MAcctSchema(ctx, 0, trxName);
        acctSchema.setAD_Org_ID(0); // 會計架構設定在 Client 層級
        acctSchema.setName(ACCT_SCHEMA_NAME);
        acctSchema.setDescription("天地人實業有限公司會計架構，使用台幣");
        acctSchema.setC_Currency_ID(currencyId);
        acctSchema.setGAAP(MAcctSchema.GAAP_InternationalGAAP); // 使用國際會計準則
        acctSchema.setCostingMethod(MAcctSchema.COSTINGMETHOD_AveragePO); // 加權平均-採購
        acctSchema.setCostingLevel(MAcctSchema.COSTINGLEVEL_Client); // 公司層級成本
        acctSchema.setAutoPeriodControl(false); // 手動帳期控制
        acctSchema.setIsAdjustCOGS(false); // 不自動調整銷貨成本
        acctSchema.setIsTradeDiscountPosted(true); // 過帳折扣
        acctSchema.setIsAccrual(true); // 使用權責發生制
        acctSchema.setM_CostType_ID(0); // 不指定成本類型

        if (!acctSchema.save()) {
            log.severe("無法儲存會計架構");
            return null;
        }

        log.info("已建立會計架構: " + ACCT_SCHEMA_NAME + " (ID=" + acctSchema.getC_AcctSchema_ID() + ")");
        return acctSchema;
    }

    /**
     * 設定 Default Accounts（預設科目）
     * <p>
     * 使用 ChartOfAccountsTW.DEFAULT_ACCOUNTS 定義的對應關係，
     * 為會計架構設定預設科目。
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param acctSchema 會計架構
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    private static boolean setupDefaultAccounts(Properties ctx, int clientId, MAcctSchema acctSchema, String trxName) {
        log.info("設定預設科目...");

        // 建立或取得 AcctSchemaDefault 記錄
        MAcctSchemaDefault acctSchemaDefault = MAcctSchemaDefault.get(ctx, acctSchema.getC_AcctSchema_ID());
        if (acctSchemaDefault == null) {
            acctSchemaDefault = new MAcctSchemaDefault(ctx, 0, trxName);
            acctSchemaDefault.setC_AcctSchema_ID(acctSchema.getC_AcctSchema_ID());
            acctSchemaDefault.setAD_Org_ID(0);
        }

        // 設定基本的預設科目（僅設定確定存在的方法）
        for (String[] defAcct : ChartOfAccountsTW.DEFAULT_ACCOUNTS) {
            String acctType = defAcct[ChartOfAccountsTW.DEF_TYPE];
            String acctValue = defAcct[ChartOfAccountsTW.DEF_ACCOUNT];

            // 取得科目 ID
            Integer elementValueId = elementValueIdMap.get(acctValue);
            if (elementValueId == null) {
                log.warning("找不到科目: " + acctValue + "，跳過設定: " + acctType);
                continue;
            }

            // 建立 Valid Combination
            MAccount account = createValidCombination(ctx, clientId, acctSchema, elementValueId, trxName);
            if (account == null) {
                log.warning("無法建立 Valid Combination for " + acctValue);
                continue;
            }

            // 根據類型設定對應的預設科目（僅設定確定支援的類型）
            setDefaultAccount(acctSchemaDefault, acctType, account.getC_ValidCombination_ID());
        }

        if (!acctSchemaDefault.save()) {
            log.severe("無法儲存預設科目設定");
            return false;
        }

        log.info("預設科目設定完成");
        return true;
    }

    /**
     * 建立 Valid Combination（有效科目組合）
     * <p>
     * Valid Combination 是 iDempiere 會計系統中的核心概念，
     * 用於將會計科目與其他維度（如組織、專案等）組合成可用於過帳的科目。
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param acctSchema 會計架構
     * @param elementValueId ElementValue ID
     * @param trxName 交易名稱
     * @return 建立的 MAccount，失敗時回傳 null
     */
    private static MAccount createValidCombination(Properties ctx, int clientId, MAcctSchema acctSchema,
                                                   int elementValueId, String trxName) {
        // 使用 MAccount.get 建立 Valid Combination
        MAccount account = MAccount.get(ctx, clientId, 0, // orgId = 0
            acctSchema.getC_AcctSchema_ID(),
            elementValueId,
            0, // C_SubAcct_ID
            0, // M_Product_ID
            0, // C_BPartner_ID
            0, // AD_OrgTrx_ID
            0, // C_LocFrom_ID
            0, // C_LocTo_ID
            0, // C_SalesRegion_ID
            0, // C_Project_ID
            0, // C_Campaign_ID
            0, // C_Activity_ID
            0, // User1_ID
            0, // User2_ID
            0, // UserElement1_ID
            0, // UserElement2_ID
            trxName);

        return account;
    }

    /**
     * 設定預設科目
     * <p>
     * 根據科目類型代碼，設定對應的預設科目欄位。
     * </p>
     *
     * @param asd AcctSchemaDefault 物件
     * @param acctType 科目類型代碼
     * @param validCombinationId Valid Combination ID
     */
    private static void setDefaultAccount(MAcctSchemaDefault asd, String acctType, int validCombinationId) {
        // 根據類型設定預設科目
        // 注意：某些方法在 iDempiere 12 中可能不存在，這裡只設定確定存在的
        switch (acctType) {
            // 應收帳款相關
            case "C_RECEIVABLE":
                asd.setC_Receivable_Acct(validCombinationId);
                break;
            case "C_PREPAYMENT":
                asd.setC_Prepayment_Acct(validCombinationId);
                break;

            // 應付帳款相關
            case "V_LIABILITY":
                asd.setV_Liability_Acct(validCombinationId);
                break;
            case "V_PREPAYMENT":
                asd.setV_Prepayment_Acct(validCombinationId);
                break;

            // 存貨相關
            case "P_ASSET":
                asd.setP_Asset_Acct(validCombinationId);
                break;
            case "P_COGS":
                asd.setP_COGS_Acct(validCombinationId);
                break;
            case "P_REVENUE":
                asd.setP_Revenue_Acct(validCombinationId);
                break;
            case "P_PURCHASEPRICEVARIANCE":
                asd.setP_PurchasePriceVariance_Acct(validCombinationId);
                break;
            case "P_INVOICEPRICEVARIANCE":
                asd.setP_InvoicePriceVariance_Acct(validCombinationId);
                break;
            case "P_TRADEDISCOUNTREC":
                asd.setP_TradeDiscountRec_Acct(validCombinationId);
                break;
            case "P_TRADEDISCOUNTGRANT":
                asd.setP_TradeDiscountGrant_Acct(validCombinationId);
                break;

            // 稅務相關
            case "T_DUE":
                asd.setT_Due_Acct(validCombinationId);
                break;
            case "T_CREDIT":
                asd.setT_Credit_Acct(validCombinationId);
                break;

            // 其他科目類型暫時跳過（某些方法在 iDempiere 12 中不存在）
            default:
                log.fine("跳過設定預設科目: " + acctType);
                break;
        }
    }

    /**
     * 取得會計架構 ID
     *
     * @return 會計架構 ID，未建立時回傳 0
     */
    public static int getAcctSchemaId() {
        return acctSchemaId;
    }

    /**
     * 取得科目表 ID
     *
     * @return 科目表 ID，未建立時回傳 0
     */
    public static int getElementId() {
        return elementId;
    }

    /**
     * 取得 ElementValue ID
     *
     * @param acctValue 科目代碼
     * @return ElementValue ID，找不到時回傳 -1
     */
    public static int getElementValueId(String acctValue) {
        Integer id = elementValueIdMap.get(acctValue);
        return id != null ? id : -1;
    }

    /**
     * 取得所有科目 ID 對照表
     *
     * @return 科目代碼對應 ID 的 Map
     */
    public static Map<String, Integer> getElementValueIdMap() {
        return new HashMap<>(elementValueIdMap);
    }
}
