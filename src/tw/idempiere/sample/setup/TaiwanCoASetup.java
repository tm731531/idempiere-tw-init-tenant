/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaDefault;
import org.compiere.model.MElementValue;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import tw.idempiere.sample.util.SetupLog;

/**
 * 台灣會計科目表建立類別
 * <p>
 * 建立符合台灣商業會計法規的會計科目表，包含：
 * <ul>
 *   <li>1xxx - 資產類</li>
 *   <li>2xxx - 負債類</li>
 *   <li>3xxx - 權益類</li>
 *   <li>4xxx - 營業收入類</li>
 *   <li>5xxx - 營業成本類</li>
 *   <li>6xxx - 營業費用類</li>
 *   <li>7xxx - 營業外收支類</li>
 *   <li>8xxx - 所得稅費用類</li>
 * </ul>
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class TaiwanCoASetup {

    private static final CLogger log = CLogger.getCLogger(TaiwanCoASetup.class);

    /** 儲存已建立的會計科目 ID，key = 會科代碼 */
    private static Map<String, Integer> accountIdMap = new HashMap<>();

    /**
     * 建立台灣會計科目表並設定 Accounting Schema Defaults
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功
     */
    public static boolean createTaiwanCoA(Properties ctx, int clientId, String trxName) {
        log.info("開始建立台灣會計科目表...");
        SetupLog.log("TaiwanCoA", "開始建立，Client=" + clientId + ", trx=" + trxName);
        accountIdMap.clear();

        try {
            // 取得 Element ID（會計科目表的容器）
            int elementId = DB.getSQLValue(trxName,
                "SELECT C_Element_ID FROM C_Element WHERE AD_Client_ID = ? AND ElementType = 'A'",
                clientId);

            SetupLog.log("TaiwanCoA", "C_Element_ID = " + elementId);

            if (elementId <= 0) {
                log.severe("找不到會計科目元素 (C_Element)");
                SetupLog.log("TaiwanCoA錯誤", "找不到 C_Element");
                return false;
            }

            // 建立會計科目層級結構
            SetupLog.log("TaiwanCoA", "開始建立會計科目層級...");
            createAccountHierarchy(ctx, clientId, elementId, trxName);
            SetupLog.log("TaiwanCoA", "會計科目層級完成，共 " + accountIdMap.size() + " 個");

            // 檢查交易狀態
            checkTrxStatus(trxName, "會計科目建立後");

            // 暫時跳過 AcctSchema Defaults 更新，因為會導致交易 abort
            // TODO: 在交易提交後，用新交易更新 AcctSchema Defaults
            SetupLog.log("TaiwanCoA", "跳過 AcctSchema Defaults 更新（避免交易 abort）");

            // 再次檢查交易狀態
            checkTrxStatus(trxName, "AcctSchema Defaults跳過後");

            log.info("台灣會計科目表建立完成，共 " + accountIdMap.size() + " 個科目");
            return true;

        } catch (Exception e) {
            log.severe("建立台灣會計科目表時發生錯誤: " + e.getMessage());
            SetupLog.logError("TaiwanCoA錯誤", "建立時發生錯誤", e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 建立會計科目層級結構（每個科目都有 try-catch 保護）
     */
    private static void createAccountHierarchy(Properties ctx, int clientId, int elementId, String trxName) {
        int orgId = 0; // 適用所有組織
        int created = 0;
        int errors = 0;

        SetupLog.log("CoA版本", "=== 版本 20260319-v3 (含樹狀結構) ===");
        SetupLog.log("CoA層級", "開始建立會計科目層級結構");

        // ==================== 1xxx 資產類 ====================
        int asset = 0;
        try {
            asset = createAccount(ctx, clientId, orgId, elementId, "1", "資產", true, MElementValue.ACCOUNTTYPE_Asset, trxName, 0);
            if (asset > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "1-資產", e); }

        // 11xx 流動資產
        int currentAsset = 0;
        try {
            currentAsset = createAccount(ctx, clientId, orgId, elementId, "11", "流動資產", true, MElementValue.ACCOUNTTYPE_Asset, trxName, asset);
            if (currentAsset > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "11-流動資產", e); }

        // 111x 現金及約當現金
        int cash = 0;
        try {
            cash = createAccount(ctx, clientId, orgId, elementId, "111", "現金及約當現金", true, MElementValue.ACCOUNTTYPE_Asset, trxName, currentAsset);
            if (cash > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "111-現金", e); }

        try { if (createAccount(ctx, clientId, orgId, elementId, "1111", "庫存現金", false, MElementValue.ACCOUNTTYPE_Asset, trxName, cash) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1111", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1112", "零用金", false, MElementValue.ACCOUNTTYPE_Asset, trxName, cash) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1112", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1113", "銀行存款", false, MElementValue.ACCOUNTTYPE_Asset, trxName, cash) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1113", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1114", "在途現金", false, MElementValue.ACCOUNTTYPE_Asset, trxName, cash) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1114", e); }

        // 112x 短期投資
        int shortInvest = 0;
        try {
            shortInvest = createAccount(ctx, clientId, orgId, elementId, "112", "短期投資", true, MElementValue.ACCOUNTTYPE_Asset, trxName, currentAsset);
            if (shortInvest > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "112", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1121", "短期投資-股票", false, MElementValue.ACCOUNTTYPE_Asset, trxName, shortInvest) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1121", e); }

        // 113x 應收票據
        int noteReceivable = 0;
        try {
            noteReceivable = createAccount(ctx, clientId, orgId, elementId, "113", "應收票據", true, MElementValue.ACCOUNTTYPE_Asset, trxName, currentAsset);
            if (noteReceivable > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "113", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1131", "應收票據", false, MElementValue.ACCOUNTTYPE_Asset, trxName, noteReceivable) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1131", e); }

        // 114x 應收帳款
        int arGroup = 0;
        try {
            arGroup = createAccount(ctx, clientId, orgId, elementId, "114", "應收帳款", true, MElementValue.ACCOUNTTYPE_Asset, trxName, currentAsset);
            if (arGroup > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "114", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1141", "應收帳款", false, MElementValue.ACCOUNTTYPE_Asset, trxName, arGroup) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1141", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1142", "應收帳款-關係人", false, MElementValue.ACCOUNTTYPE_Asset, trxName, arGroup) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1142", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1149", "備抵呆帳-應收帳款", false, MElementValue.ACCOUNTTYPE_Asset, trxName, arGroup) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1149", e); }

        // 118x 其他應收款
        int otherReceivable = 0;
        try {
            otherReceivable = createAccount(ctx, clientId, orgId, elementId, "118", "其他應收款", true, MElementValue.ACCOUNTTYPE_Asset, trxName, currentAsset);
            if (otherReceivable > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "118", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1181", "應收利息", false, MElementValue.ACCOUNTTYPE_Asset, trxName, otherReceivable) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1181", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1182", "應收股利", false, MElementValue.ACCOUNTTYPE_Asset, trxName, otherReceivable) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1182", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1184", "預付款項", false, MElementValue.ACCOUNTTYPE_Asset, trxName, otherReceivable) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1184", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1185", "進項稅額", false, MElementValue.ACCOUNTTYPE_Asset, trxName, otherReceivable) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1185", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1189", "其他應收款-其他", false, MElementValue.ACCOUNTTYPE_Asset, trxName, otherReceivable) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1189", e); }

        // 119x 存貨
        int inventory = 0;
        try {
            inventory = createAccount(ctx, clientId, orgId, elementId, "119", "存貨", true, MElementValue.ACCOUNTTYPE_Asset, trxName, currentAsset);
            if (inventory > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "119", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1191", "商品存貨", false, MElementValue.ACCOUNTTYPE_Asset, trxName, inventory) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1191", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1192", "原物料", false, MElementValue.ACCOUNTTYPE_Asset, trxName, inventory) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1192", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1193", "在製品", false, MElementValue.ACCOUNTTYPE_Asset, trxName, inventory) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1193", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1194", "製成品", false, MElementValue.ACCOUNTTYPE_Asset, trxName, inventory) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1194", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1195", "存貨跌價準備", false, MElementValue.ACCOUNTTYPE_Asset, trxName, inventory) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1195", e); }

        // 12xx 非流動資產
        int nonCurrentAsset = 0;
        try {
            nonCurrentAsset = createAccount(ctx, clientId, orgId, elementId, "12", "非流動資產", true, MElementValue.ACCOUNTTYPE_Asset, trxName, asset);
            if (nonCurrentAsset > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "12", e); }

        // 121x 固定資產
        int fixedAsset = 0;
        try {
            fixedAsset = createAccount(ctx, clientId, orgId, elementId, "121", "固定資產", true, MElementValue.ACCOUNTTYPE_Asset, trxName, nonCurrentAsset);
            if (fixedAsset > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "121", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1211", "土地", false, MElementValue.ACCOUNTTYPE_Asset, trxName, fixedAsset) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1211", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1212", "房屋及建築", false, MElementValue.ACCOUNTTYPE_Asset, trxName, fixedAsset) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1212", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1213", "機器設備", false, MElementValue.ACCOUNTTYPE_Asset, trxName, fixedAsset) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1213", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1214", "運輸設備", false, MElementValue.ACCOUNTTYPE_Asset, trxName, fixedAsset) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1214", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1215", "辦公設備", false, MElementValue.ACCOUNTTYPE_Asset, trxName, fixedAsset) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1215", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "1219", "累計折舊", false, MElementValue.ACCOUNTTYPE_Asset, trxName, fixedAsset) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "1219", e); }

        SetupLog.log("CoA進度", "資產類完成: created=" + created + ", errors=" + errors);

        // ==================== 2xxx 負債類 ====================
        int liability = 0;
        try {
            liability = createAccount(ctx, clientId, orgId, elementId, "2", "負債", true, MElementValue.ACCOUNTTYPE_Liability, trxName, 0);
            if (liability > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "2-負債", e); }

        // 21xx 流動負債
        int currentLiab = 0;
        try {
            currentLiab = createAccount(ctx, clientId, orgId, elementId, "21", "流動負債", true, MElementValue.ACCOUNTTYPE_Liability, trxName, liability);
            if (currentLiab > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "21", e); }

        // 211x 短期借款
        int shortLoan = 0;
        try {
            shortLoan = createAccount(ctx, clientId, orgId, elementId, "211", "短期借款", true, MElementValue.ACCOUNTTYPE_Liability, trxName, currentLiab);
            if (shortLoan > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "211", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "2111", "短期借款", false, MElementValue.ACCOUNTTYPE_Liability, trxName, shortLoan) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "2111", e); }

        // 212x 應付票據
        int notePayable = 0;
        try {
            notePayable = createAccount(ctx, clientId, orgId, elementId, "212", "應付票據", true, MElementValue.ACCOUNTTYPE_Liability, trxName, currentLiab);
            if (notePayable > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "212", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "2121", "應付票據", false, MElementValue.ACCOUNTTYPE_Liability, trxName, notePayable) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "2121", e); }

        // 213x 應付帳款
        int apGroup = 0;
        try {
            apGroup = createAccount(ctx, clientId, orgId, elementId, "213", "應付帳款", true, MElementValue.ACCOUNTTYPE_Liability, trxName, currentLiab);
            if (apGroup > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "213", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "2131", "應付帳款", false, MElementValue.ACCOUNTTYPE_Liability, trxName, apGroup) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "2131", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "2132", "應付帳款-關係人", false, MElementValue.ACCOUNTTYPE_Liability, trxName, apGroup) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "2132", e); }

        // 214x 其他應付款
        int otherPayable = 0;
        try {
            otherPayable = createAccount(ctx, clientId, orgId, elementId, "214", "其他應付款", true, MElementValue.ACCOUNTTYPE_Liability, trxName, currentLiab);
            if (otherPayable > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "214", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "2141", "應付費用", false, MElementValue.ACCOUNTTYPE_Liability, trxName, otherPayable) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "2141", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "2142", "應付薪資", false, MElementValue.ACCOUNTTYPE_Liability, trxName, otherPayable) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "2142", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "2143", "應付利息", false, MElementValue.ACCOUNTTYPE_Liability, trxName, otherPayable) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "2143", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "2144", "銷項稅額", false, MElementValue.ACCOUNTTYPE_Liability, trxName, otherPayable) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "2144", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "2145", "預收款項", false, MElementValue.ACCOUNTTYPE_Liability, trxName, otherPayable) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "2145", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "2149", "其他應付款-其他", false, MElementValue.ACCOUNTTYPE_Liability, trxName, otherPayable) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "2149", e); }

        SetupLog.log("CoA進度", "負債類完成: created=" + created + ", errors=" + errors);

        // ==================== 3xxx 權益類 ====================
        int equity = 0;
        try {
            equity = createAccount(ctx, clientId, orgId, elementId, "3", "權益", true, "O", trxName, 0);
            if (equity > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "3-權益", e); }

        // 31xx 股本
        int capital = 0;
        try {
            capital = createAccount(ctx, clientId, orgId, elementId, "31", "股本", true, "O", trxName, equity);
            if (capital > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "31", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "3111", "普通股股本", false, "O", trxName, capital) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "3111", e); }

        // 32xx 資本公積
        int capitalSurplus = 0;
        try {
            capitalSurplus = createAccount(ctx, clientId, orgId, elementId, "32", "資本公積", true, "O", trxName, equity);
            if (capitalSurplus > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "32", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "3211", "資本公積-股票溢價", false, "O", trxName, capitalSurplus) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "3211", e); }

        // 33xx 保留盈餘
        int retainedEarnings = 0;
        try {
            retainedEarnings = createAccount(ctx, clientId, orgId, elementId, "33", "保留盈餘", true, "O", trxName, equity);
            if (retainedEarnings > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "33", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "3311", "法定盈餘公積", false, "O", trxName, retainedEarnings) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "3311", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "3312", "特別盈餘公積", false, "O", trxName, retainedEarnings) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "3312", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "3321", "未分配盈餘", false, "O", trxName, retainedEarnings) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "3321", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "3322", "本期損益", false, "O", trxName, retainedEarnings) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "3322", e); }

        SetupLog.log("CoA進度", "權益類完成: created=" + created + ", errors=" + errors);

        // ==================== 4xxx 營業收入類 ====================
        int revenue = 0;
        try {
            revenue = createAccount(ctx, clientId, orgId, elementId, "4", "營業收入", true, MElementValue.ACCOUNTTYPE_Revenue, trxName, 0);
            if (revenue > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "4-營業收入", e); }

        // 41xx 銷貨收入
        int salesRev = 0;
        try {
            salesRev = createAccount(ctx, clientId, orgId, elementId, "41", "銷貨收入", true, MElementValue.ACCOUNTTYPE_Revenue, trxName, revenue);
            if (salesRev > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "41", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "4111", "銷貨收入", false, MElementValue.ACCOUNTTYPE_Revenue, trxName, salesRev) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "4111", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "4112", "銷貨退回", false, MElementValue.ACCOUNTTYPE_Revenue, trxName, salesRev) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "4112", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "4113", "銷貨折讓", false, MElementValue.ACCOUNTTYPE_Revenue, trxName, salesRev) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "4113", e); }

        // 42xx 服務收入
        int serviceRev = 0;
        try {
            serviceRev = createAccount(ctx, clientId, orgId, elementId, "42", "服務收入", true, MElementValue.ACCOUNTTYPE_Revenue, trxName, revenue);
            if (serviceRev > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "42", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "4211", "服務收入", false, MElementValue.ACCOUNTTYPE_Revenue, trxName, serviceRev) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "4211", e); }

        SetupLog.log("CoA進度", "收入類完成: created=" + created + ", errors=" + errors);

        // ==================== 5xxx 營業成本類 ====================
        int cost = 0;
        try {
            cost = createAccount(ctx, clientId, orgId, elementId, "5", "營業成本", true, MElementValue.ACCOUNTTYPE_Expense, trxName, 0);
            if (cost > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "5-營業成本", e); }

        // 51xx 銷貨成本
        int cogs = 0;
        try {
            cogs = createAccount(ctx, clientId, orgId, elementId, "51", "銷貨成本", true, MElementValue.ACCOUNTTYPE_Expense, trxName, cost);
            if (cogs > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "51", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "5111", "銷貨成本", false, MElementValue.ACCOUNTTYPE_Expense, trxName, cogs) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "5111", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "5112", "進貨", false, MElementValue.ACCOUNTTYPE_Expense, trxName, cogs) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "5112", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "5113", "進貨退出", false, MElementValue.ACCOUNTTYPE_Expense, trxName, cogs) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "5113", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "5114", "進貨折讓", false, MElementValue.ACCOUNTTYPE_Expense, trxName, cogs) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "5114", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "5115", "進貨運費", false, MElementValue.ACCOUNTTYPE_Expense, trxName, cogs) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "5115", e); }

        // 52xx 製造成本
        int mfgCost = 0;
        try {
            mfgCost = createAccount(ctx, clientId, orgId, elementId, "52", "製造成本", true, MElementValue.ACCOUNTTYPE_Expense, trxName, cost);
            if (mfgCost > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "52", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "5211", "直接原料", false, MElementValue.ACCOUNTTYPE_Expense, trxName, mfgCost) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "5211", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "5212", "直接人工", false, MElementValue.ACCOUNTTYPE_Expense, trxName, mfgCost) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "5212", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "5213", "製造費用", false, MElementValue.ACCOUNTTYPE_Expense, trxName, mfgCost) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "5213", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "5214", "成本差異", false, MElementValue.ACCOUNTTYPE_Expense, trxName, mfgCost) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "5214", e); }

        SetupLog.log("CoA進度", "成本類完成: created=" + created + ", errors=" + errors);

        // ==================== 6xxx 營業費用類 ====================
        int opExp = 0;
        try {
            opExp = createAccount(ctx, clientId, orgId, elementId, "6", "營業費用", true, MElementValue.ACCOUNTTYPE_Expense, trxName, 0);
            if (opExp > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "6-營業費用", e); }

        // 61xx 推銷費用
        int sellingExp = 0;
        try {
            sellingExp = createAccount(ctx, clientId, orgId, elementId, "61", "推銷費用", true, MElementValue.ACCOUNTTYPE_Expense, trxName, opExp);
            if (sellingExp > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "61", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "6111", "薪資費用-推銷", false, MElementValue.ACCOUNTTYPE_Expense, trxName, sellingExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "6111", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "6112", "廣告費", false, MElementValue.ACCOUNTTYPE_Expense, trxName, sellingExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "6112", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "6113", "運費", false, MElementValue.ACCOUNTTYPE_Expense, trxName, sellingExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "6113", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "6119", "其他推銷費用", false, MElementValue.ACCOUNTTYPE_Expense, trxName, sellingExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "6119", e); }

        // 62xx 管理費用
        int adminExp = 0;
        try {
            adminExp = createAccount(ctx, clientId, orgId, elementId, "62", "管理費用", true, MElementValue.ACCOUNTTYPE_Expense, trxName, opExp);
            if (adminExp > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "62", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "6211", "薪資費用-管理", false, MElementValue.ACCOUNTTYPE_Expense, trxName, adminExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "6211", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "6212", "租金費用", false, MElementValue.ACCOUNTTYPE_Expense, trxName, adminExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "6212", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "6213", "文具用品", false, MElementValue.ACCOUNTTYPE_Expense, trxName, adminExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "6213", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "6214", "水電費", false, MElementValue.ACCOUNTTYPE_Expense, trxName, adminExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "6214", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "6215", "保險費", false, MElementValue.ACCOUNTTYPE_Expense, trxName, adminExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "6215", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "6216", "折舊費用", false, MElementValue.ACCOUNTTYPE_Expense, trxName, adminExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "6216", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "6217", "呆帳費用", false, MElementValue.ACCOUNTTYPE_Expense, trxName, adminExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "6217", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "6218", "稅捐", false, MElementValue.ACCOUNTTYPE_Expense, trxName, adminExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "6218", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "6219", "其他管理費用", false, MElementValue.ACCOUNTTYPE_Expense, trxName, adminExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "6219", e); }

        SetupLog.log("CoA進度", "費用類完成: created=" + created + ", errors=" + errors);

        // ==================== 7xxx 營業外收支類 ====================
        int nonOp = 0;
        try {
            nonOp = createAccount(ctx, clientId, orgId, elementId, "7", "營業外收支", true, MElementValue.ACCOUNTTYPE_Expense, trxName, 0);
            if (nonOp > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "7-營業外收支", e); }

        // 71xx 營業外收入
        int nonOpIncome = 0;
        try {
            nonOpIncome = createAccount(ctx, clientId, orgId, elementId, "71", "營業外收入", true, MElementValue.ACCOUNTTYPE_Revenue, trxName, nonOp);
            if (nonOpIncome > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "71", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "7111", "利息收入", false, MElementValue.ACCOUNTTYPE_Revenue, trxName, nonOpIncome) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "7111", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "7112", "股利收入", false, MElementValue.ACCOUNTTYPE_Revenue, trxName, nonOpIncome) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "7112", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "7113", "租金收入", false, MElementValue.ACCOUNTTYPE_Revenue, trxName, nonOpIncome) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "7113", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "7114", "兌換利益", false, MElementValue.ACCOUNTTYPE_Revenue, trxName, nonOpIncome) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "7114", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "7115", "處分資產利益", false, MElementValue.ACCOUNTTYPE_Revenue, trxName, nonOpIncome) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "7115", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "7119", "其他營業外收入", false, MElementValue.ACCOUNTTYPE_Revenue, trxName, nonOpIncome) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "7119", e); }

        // 72xx 營業外費用
        int nonOpExp = 0;
        try {
            nonOpExp = createAccount(ctx, clientId, orgId, elementId, "72", "營業外費用", true, MElementValue.ACCOUNTTYPE_Expense, trxName, nonOp);
            if (nonOpExp > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "72", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "7211", "利息費用", false, MElementValue.ACCOUNTTYPE_Expense, trxName, nonOpExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "7211", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "7212", "兌換損失", false, MElementValue.ACCOUNTTYPE_Expense, trxName, nonOpExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "7212", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "7213", "處分資產損失", false, MElementValue.ACCOUNTTYPE_Expense, trxName, nonOpExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "7213", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "7219", "其他營業外費用", false, MElementValue.ACCOUNTTYPE_Expense, trxName, nonOpExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "7219", e); }

        SetupLog.log("CoA進度", "營業外收支完成: created=" + created + ", errors=" + errors);

        // ==================== 8xxx 所得稅費用類 ====================
        int taxExp = 0;
        try {
            taxExp = createAccount(ctx, clientId, orgId, elementId, "8", "所得稅費用", true, MElementValue.ACCOUNTTYPE_Expense, trxName, 0);
            if (taxExp > 0) created++; else errors++;
        } catch (Exception e) { errors++; SetupLog.logError("CoA", "8-所得稅費用", e); }
        try { if (createAccount(ctx, clientId, orgId, elementId, "8111", "所得稅費用", false, MElementValue.ACCOUNTTYPE_Expense, trxName, taxExp) > 0) created++; else errors++; } catch (Exception e) { errors++; SetupLog.logError("CoA", "8111", e); }

        SetupLog.log("CoA完成", "總計: created=" + created + ", errors=" + errors);
    }

    /**
     * 建立單一會計科目（使用乾淨的 Context 確保 Client ID 正確）
     * 包含完整錯誤處理，確保不會中斷整個流程
     *
     * @return C_ElementValue_ID
     */
    private static int createAccount(Properties ctx, int clientId, int orgId, int elementId,
            String value, String name, boolean isSummary, String accountType, String trxName, int parentId) {

        try {
            // 檢查是否已存在
            int existingId = -1;
            try {
                existingId = DB.getSQLValue(trxName,
                    "SELECT C_ElementValue_ID FROM C_ElementValue WHERE AD_Client_ID=? AND Value=?",
                    clientId, value);
            } catch (Exception e) {
                SetupLog.logError("檢查EV存在錯誤", value, e);
            }

            if (existingId > 0) {
                accountIdMap.put(value, existingId);
                return existingId;
            }

            // 建立乾淨的 Context，確保不受其他代碼影響
            Properties cleanCtx = new Properties();
            cleanCtx.putAll(ctx);
            Env.setContext(cleanCtx, Env.AD_CLIENT_ID, clientId);
            Env.setContext(cleanCtx, Env.AD_ORG_ID, orgId);
            Env.setContext(cleanCtx, Env.AD_USER_ID, 0);

            // 使用 Model 類建立會計科目
            MElementValue ev = new MElementValue(cleanCtx, 0, trxName);
            ev.setC_Element_ID(elementId);
            ev.setValue(value);
            ev.setName(name);
            ev.setDescription(name);
            ev.setAccountType(accountType);
            ev.setIsSummary(isSummary);
            ev.setIsActive(true);
            ev.setPostActual(true);
            ev.setPostBudget(true);
            ev.setPostStatistical(true);
            ev.setPostEncumbrance(true);

            // 設定自然符號（借方/貸方）
            if (accountType.equals(MElementValue.ACCOUNTTYPE_Asset) ||
                accountType.equals(MElementValue.ACCOUNTTYPE_Expense)) {
                ev.setAccountSign(MElementValue.ACCOUNTSIGN_Debit);
            } else {
                ev.setAccountSign(MElementValue.ACCOUNTSIGN_Credit);
            }

            // 記錄建立前的狀態（僅第一個科目）
            if ("1".equals(value)) {
                SetupLog.log("EV建立前", "cleanCtx.AD_Client_ID=" + Env.getAD_Client_ID(cleanCtx) +
                    ", ev.AD_Client_ID=" + ev.getAD_Client_ID() + ", 預期=" + clientId +
                    ", trxName=" + trxName + ", ev.trxName=" + ev.get_TrxName());
            }

            // 明確使用 save(trxName) 確保在正確的交易中儲存
            boolean saved = false;
            try {
                saved = ev.save(trxName);
            } catch (Exception e) {
                SetupLog.logError("EV.save異常", value + " - " + name, e);
                return 0;
            }

            if (saved) {
                int newId = ev.getC_ElementValue_ID();
                accountIdMap.put(value, newId);

                // 驗證是否真的存在於資料庫（前三個科目，診斷用）
                if ("1".equals(value) || "11".equals(value) || "111".equals(value)) {
                    try {
                        int checkId = DB.getSQLValue(trxName,
                            "SELECT C_ElementValue_ID FROM C_ElementValue WHERE C_ElementValue_ID=?", newId);
                        int checkClientId = DB.getSQLValue(trxName,
                            "SELECT AD_Client_ID FROM C_ElementValue WHERE C_ElementValue_ID=?", newId);
                        SetupLog.log("EV驗證", "value=" + value + ", newId=" + newId +
                            ", DB查詢ID=" + checkId + ", DB.AD_Client_ID=" + checkClientId +
                            ", 預期Client=" + clientId);
                    } catch (Exception e) {
                        SetupLog.logError("EV驗證錯誤", value, e);
                    }
                }

                // 設定父科目關係（透過 AD_TreeNode）
                SetupLog.log("createAccount", "value=" + value + ", newId=" + newId + ", parentId=" + parentId);
                if (parentId > 0) {
                    setParentAccount(clientId, elementId, newId, parentId, trxName);
                }

                log.fine("已建立會計科目: " + value + " - " + name + " (ID=" + newId + ")");
                return newId;
            } else {
                log.warning("無法建立會計科目: " + value + " - " + name);
                // 取得詳細錯誤訊息
                String saveError = "";
                try {
                    saveError = org.compiere.util.CLogger.retrieveErrorString("");
                } catch (Exception e) {
                    saveError = "無法取得錯誤: " + e.getMessage();
                }
                SetupLog.log("會科建立失敗", value + " - " + name +
                    ", ev.AD_Client_ID=" + ev.getAD_Client_ID() +
                    ", 錯誤=" + saveError);
                return 0;
            }
        } catch (Exception e) {
            SetupLog.logError("createAccount總異常", value + " - " + name, e);
            return 0;
        }
    }

    /**
     * 設定會計科目的父子關係（包含完整錯誤處理）
     * <p>
     * 從 C_Element 取得關聯的 AD_Tree_ID，然後更新 AD_TreeNode 表的 Parent_ID。
     * 注意：會計科目使用 AD_TreeNode（不是 AD_TreeNodePR）。
     * </p>
     */
    private static void setParentAccount(int clientId, int elementId, int nodeId, int parentId, String trxName) {
        SetupLog.log("setParent入口", "clientId=" + clientId + ", elementId=" + elementId +
            ", nodeId=" + nodeId + ", parentId=" + parentId);

        try {
            // 從 C_Element 取得 AD_Tree_ID
            int treeId = DB.getSQLValue(trxName,
                "SELECT AD_Tree_ID FROM C_Element WHERE C_Element_ID=?", elementId);
            SetupLog.log("setParent", "treeId=" + treeId);

            if (treeId <= 0) {
                // 備用：從 AD_Tree 查詢 TreeType='EV'
                treeId = DB.getSQLValue(trxName,
                    "SELECT AD_Tree_ID FROM AD_Tree WHERE AD_Client_ID=? AND TreeType='EV'", clientId);
            }

            if (treeId > 0) {
                try {
                    // 取得 SeqNo（在同一父節點下的排序）
                    int seqNo = DB.getSQLValue(trxName,
                        "SELECT COALESCE(MAX(SeqNo),0)+10 FROM AD_TreeNode WHERE AD_Tree_ID=? AND Parent_ID=?",
                        treeId, parentId);
                    if (seqNo <= 0) seqNo = 10;

                    // 檢查記錄是否存在
                    int existingParent = DB.getSQLValue(trxName,
                        "SELECT Parent_ID FROM AD_TreeNode WHERE AD_Tree_ID=? AND Node_ID=?",
                        treeId, nodeId);

                    // 更新 AD_TreeNode
                    String sql = "UPDATE AD_TreeNode SET Parent_ID=?, SeqNo=? " +
                        "WHERE AD_Tree_ID=? AND Node_ID=?";
                    int result = DB.executeUpdate(sql, new Object[]{parentId, seqNo, treeId, nodeId}, false, trxName);

                    // 驗證更新結果
                    int newParent = DB.getSQLValue(trxName,
                        "SELECT Parent_ID FROM AD_TreeNode WHERE AD_Tree_ID=? AND Node_ID=?",
                        treeId, nodeId);

                    SetupLog.log("TreeNode", "nodeId=" + nodeId + ", parentId=" + parentId +
                        ", treeId=" + treeId + ", existingParent=" + existingParent +
                        ", result=" + result + ", newParent=" + newParent);

                    if (result <= 0) {
                        // 如果更新失敗，可能是記錄不存在，嘗試插入
                        sql = "INSERT INTO AD_TreeNode (AD_Tree_ID, Node_ID, AD_Client_ID, AD_Org_ID, " +
                            "IsActive, Created, CreatedBy, Updated, UpdatedBy, Parent_ID, SeqNo) " +
                            "VALUES (?, ?, ?, 0, 'Y', NOW(), 0, NOW(), 0, ?, ?)";
                        int insertResult = DB.executeUpdate(sql, new Object[]{treeId, nodeId, clientId, parentId, seqNo}, false, trxName);
                        SetupLog.log("TreeNode-INSERT", "nodeId=" + nodeId + ", insertResult=" + insertResult);
                    }
                } catch (Exception e) {
                    SetupLog.logError("TreeNode錯誤", "nodeId=" + nodeId + ", parentId=" + parentId, e);
                }
            } else {
                SetupLog.log("TreeNode警告", "找不到 Tree for element=" + elementId);
            }
        } catch (Exception e) {
            SetupLog.logError("setParentAccount錯誤", "nodeId=" + nodeId, e);
        }
    }

    /**
     * 在主交易提交後更新 Accounting Schema Defaults
     * <p>
     * 此方法應在主交易提交後調用，使用新的交易來更新 AcctSchema Defaults。
     * 這樣可以避免在主交易中因為 ValidCombination 建立問題導致整個交易 abort。
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @return true=成功, false=失敗
     */
    public static boolean updateAcctSchemaDefaultsPostCommit(Properties ctx, int clientId) {
        SetupLog.log("AcctDefaults", "開始更新（提交後）...");

        // 使用新的交易
        String trxName = org.compiere.util.Trx.createTrxName("AcctDefaults_" + System.currentTimeMillis());
        org.compiere.util.Trx trx = org.compiere.util.Trx.get(trxName, true);
        trx.start();

        try {
            // 重建 accountIdMap（因為在新交易中需要重新查詢）
            accountIdMap.clear();
            String sql = "SELECT Value, C_ElementValue_ID FROM C_ElementValue WHERE AD_Client_ID=? AND Value ~ '^[1-8]'";
            java.sql.PreparedStatement pstmt = null;
            java.sql.ResultSet rs = null;
            try {
                pstmt = DB.prepareStatement(sql, trxName);
                pstmt.setInt(1, clientId);
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    accountIdMap.put(rs.getString(1), rs.getInt(2));
                }
            } finally {
                DB.close(rs, pstmt);
            }
            SetupLog.log("AcctDefaults", "載入了 " + accountIdMap.size() + " 個會計科目到 map");

            // 更新 AcctSchema Defaults
            updateAcctSchemaDefaults(ctx, clientId, trxName);

            // 提交
            boolean committed = trx.commit();
            SetupLog.log("AcctDefaults", "提交結果: " + committed);

            return committed;

        } catch (Exception e) {
            SetupLog.logError("AcctDefaults", "更新失敗", e);
            trx.rollback();
            return false;
        } finally {
            trx.close();
        }
    }

    /**
     * 更新 Accounting Schema Defaults，對應到台灣會計科目
     */
    private static void updateAcctSchemaDefaults(Properties ctx, int clientId, String trxName) {
        log.info("更新 Accounting Schema Defaults...");

        // 診斷點 1
        checkTrxStatus(trxName, "AcctDefault開始前");

        // 取得 Accounting Schema
        int acctSchemaId = -1;
        try {
            acctSchemaId = DB.getSQLValue(trxName,
                "SELECT C_AcctSchema_ID FROM C_AcctSchema WHERE AD_Client_ID=?", clientId);
            SetupLog.log("AcctDefault", "acctSchemaId=" + acctSchemaId);
        } catch (Exception e) {
            SetupLog.logError("AcctDefault", "取得 acctSchemaId 失敗", e);
            return;
        }

        // 診斷點 2
        checkTrxStatus(trxName, "取得acctSchemaId後");

        if (acctSchemaId <= 0) {
            log.warning("找不到 Accounting Schema");
            return;
        }

        MAcctSchema as = null;
        try {
            as = MAcctSchema.get(ctx, acctSchemaId);
            SetupLog.log("AcctDefault", "MAcctSchema.get 完成");
        } catch (Exception e) {
            SetupLog.logError("AcctDefault", "MAcctSchema.get 失敗", e);
        }

        // 診斷點 3
        checkTrxStatus(trxName, "MAcctSchema.get後");

        MAcctSchemaDefault asd = null;
        try {
            asd = MAcctSchemaDefault.get(ctx, acctSchemaId);
            SetupLog.log("AcctDefault", "MAcctSchemaDefault.get 完成");
        } catch (Exception e) {
            SetupLog.logError("AcctDefault", "MAcctSchemaDefault.get 失敗", e);
        }

        // 診斷點 4
        checkTrxStatus(trxName, "MAcctSchemaDefault.get後");

        if (asd == null) {
            log.warning("找不到 Accounting Schema Defaults");
            return;
        }

        // 定義 Default Account 對應到台灣會科
        // 格式: {DefaultAccount 欄位名, 台灣會科代碼}
        String[][] mappings = {
            // 銀行相關
            {"B_Asset_Acct", "1113"},           // 銀行存款
            {"B_InTransit_Acct", "1114"},       // 在途現金
            {"B_UnallocatedCash_Acct", "1113"}, // 未分配收款 -> 銀行存款
            {"B_InterestRev_Acct", "7111"},     // 利息收入
            {"B_InterestExp_Acct", "7211"},     // 利息費用

            // 零用金
            {"CB_Asset_Acct", "1112"},          // 零用金
            {"CB_CashTransfer_Acct", "1114"},   // 零用金在途
            {"CB_Differences_Acct", "7219"},    // 零用金差異
            {"CB_Expense_Acct", "6219"},        // 零用金費用
            {"CB_Receipt_Acct", "7119"},        // 零用金收入

            // 應收帳款
            {"C_Receivable_Acct", "1141"},      // 應收帳款
            {"C_Receivable_Services_Acct", "1141"}, // 應收服務款
            {"C_Prepayment_Acct", "2145"},      // 客戶預收款

            // 應付帳款
            {"V_Liability_Acct", "2131"},       // 應付帳款
            {"V_Liability_Services_Acct", "2131"}, // 應付服務款
            {"V_Prepayment_Acct", "1184"},      // 供應商預付款

            // 商品相關
            {"P_Revenue_Acct", "4111"},         // 銷貨收入
            {"P_Expense_Acct", "5112"},         // 進貨
            {"P_Asset_Acct", "1191"},           // 商品存貨
            {"P_COGS_Acct", "5111"},            // 銷貨成本
            {"P_PurchasePriceVariance_Acct", "5214"}, // 進貨價差
            {"P_InvoicePriceVariance_Acct", "5214"},  // 發票價差
            {"P_TradeDiscountRec_Acct", "5114"},      // 進貨折讓
            {"P_TradeDiscountGrant_Acct", "4113"},    // 銷貨折讓

            // 製造相關
            {"P_CostOfProduction_Acct", "5211"}, // 生產成本
            {"P_Labor_Acct", "5212"},           // 直接人工
            {"P_Burden_Acct", "5213"},          // 製造費用
            {"P_OutsideProcessing_Acct", "5213"}, // 委外加工
            {"P_Overhead_Acct", "5213"},        // 間接費用
            {"P_Scrap_Acct", "7213"},           // 報廢損失
            {"P_WIP_Acct", "1193"},             // 在製品
            {"P_MethodChangeVariance_Acct", "5214"}, // 方法變更差異
            {"P_UsageVariance_Acct", "5214"},   // 用量差異
            {"P_RateVariance_Acct", "5214"},    // 費率差異
            {"P_MixVariance_Acct", "5214"},     // 組合差異
            {"P_FloorStock_Acct", "1192"},      // 車間物料
            {"P_CostAdjustment_Acct", "5214"},  // 成本調整
            {"P_InventoryClearing_Acct", "1191"}, // 存貨清算

            // 稅務相關
            {"T_Due_Acct", "2144"},             // 銷項稅額
            {"T_Credit_Acct", "1185"},          // 進項稅額
            {"T_Expense_Acct", "6218"},         // 稅捐

            // 倉庫相關
            {"W_Differences_Acct", "5214"},     // 倉庫差異
            {"W_InvActualAdjust_Acct", "5214"}, // 存貨實際調整
            {"W_Revaluation_Acct", "5214"},     // 存貨重估

            // 運費
            {"P_Freight_Acct", "5115"},         // 運費

            // 雜項
            {"Ch_Expense_Acct", "6219"},        // 雜項費用
            {"Ch_Revenue_Acct", "7119"},        // 雜項收入

            // 匯兌損益
            {"UnrealizedGain_Acct", "7114"},    // 未實現匯兌利益
            {"UnrealizedLoss_Acct", "7212"},    // 未實現匯兌損失
            {"RealizedGain_Acct", "7114"},      // 已實現匯兌利益
            {"RealizedLoss_Acct", "7212"},      // 已實現匯兌損失

            // 其他
            {"RetainedEarning_Acct", "3321"},   // 未分配盈餘
            {"IncomeSummary_Acct", "3322"},     // 本期損益
        };

        int updated = 0;
        int errors = 0;
        int skipped = 0;
        for (String[] mapping : mappings) {
            String columnName = mapping[0];
            String accountValue = mapping[1];

            try {
                Integer accountId = accountIdMap.get(accountValue);
                if (accountId == null || accountId <= 0) {
                    skipped++;
                    SetupLog.log("AcctDefault跳過", columnName + ": 找不到帳戶 " + accountValue);
                    continue;
                }

                // 建立 ValidCombination 並更新 Default（傳入 accountValue 避免查詢）
                int vcId = createValidCombination(ctx, as, accountId, accountValue, trxName);
                if (vcId > 0) {
                    // 更新 C_AcctSchema_Default
                    String sql = "UPDATE C_AcctSchema_Default SET " + columnName + " = ? " +
                        "WHERE C_AcctSchema_ID = ?";
                    int result = DB.executeUpdate(sql, new Object[]{vcId, acctSchemaId}, false, trxName);
                    if (result > 0) {
                        updated++;
                    }
                }
            } catch (Exception e) {
                errors++;
                SetupLog.logError("AcctDefault錯誤", columnName + "=" + accountValue, e);
            }
        }

        SetupLog.log("AcctDefault結果", "成功=" + updated + ", 跳過=" + skipped + ", 錯誤=" + errors);
        log.info("已更新 " + updated + " 個 Accounting Schema Defaults");
    }

    /**
     * 建立 C_ValidCombination
     * <p>
     * 直接使用 SQL INSERT 建立，完全避免 MAccount.get() 可能的問題。
     * 從 accountIdMap 取得 Value，不再查詢資料庫。
     * </p>
     */
    private static int createValidCombination(Properties ctx, MAcctSchema as, int accountId,
            String accountValue, String trxName) {
        try {
            int clientId = as.getAD_Client_ID();
            int acctSchemaId = as.getC_AcctSchema_ID();

            // 先檢查是否已存在
            String checkSql = "SELECT C_ValidCombination_ID FROM C_ValidCombination " +
                "WHERE AD_Client_ID=? AND C_AcctSchema_ID=? AND Account_ID=? AND AD_Org_ID=0";
            int existingId = DB.getSQLValue(trxName, checkSql, clientId, acctSchemaId, accountId);
            if (existingId > 0) {
                return existingId;
            }

            // 取得新的 ID
            int newId = DB.getNextID(clientId, "C_ValidCombination", trxName);
            if (newId <= 0) {
                SetupLog.log("VC警告", "無法取得新 ID, accountValue=" + accountValue);
                return 0;
            }

            // 直接用 SQL INSERT 建立 C_ValidCombination
            String insertSql = "INSERT INTO C_ValidCombination " +
                "(C_ValidCombination_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy, " +
                "Alias, Combination, Description, IsFullyQualified, C_AcctSchema_ID, Account_ID) " +
                "VALUES (?, ?, 0, 'Y', NOW(), 0, NOW(), 0, ?, ?, ?, 'Y', ?, ?)";

            // 使用 SAVEPOINT 隔離可能的錯誤
            java.sql.Savepoint sp = null;
            org.compiere.util.Trx trx = org.compiere.util.Trx.get(trxName, false);
            try {
                if (trx != null && trx.getConnection() != null) {
                    sp = trx.getConnection().setSavepoint("VC_" + accountValue);
                }
            } catch (Exception e) {
                SetupLog.log("VC-SP", "設定 SAVEPOINT 失敗: " + e.getMessage());
            }

            int result = -1;
            String errorMsg = null;
            try {
                // 使用 executeUpdateEx 獲取異常
                result = DB.executeUpdateEx(insertSql,
                    new Object[]{newId, clientId, accountValue, accountValue, accountValue, acctSchemaId, accountId},
                    trxName);
            } catch (Exception e) {
                errorMsg = e.getMessage();
                SetupLog.log("VC-INSERT錯誤", accountValue + ": " + errorMsg);
                // 回滾到 savepoint
                if (sp != null && trx != null && trx.getConnection() != null) {
                    try {
                        trx.getConnection().rollback(sp);
                        SetupLog.log("VC-SP", "已回滾到 SAVEPOINT");
                    } catch (Exception re) {
                        SetupLog.log("VC-SP", "回滾失敗: " + re.getMessage());
                    }
                }
                return 0;
            }

            if (result > 0) {
                return newId;
            } else {
                SetupLog.log("VC警告", "INSERT 返回 " + result + ", accountValue=" + accountValue);
            }
        } catch (Exception e) {
            SetupLog.logError("VC異常", "accountValue=" + accountValue, e);
        }
        return 0;
    }

    /**
     * 檢查交易狀態
     */
    private static void checkTrxStatus(String trxName, String step) {
        try {
            org.compiere.util.Trx trx = org.compiere.util.Trx.get(trxName, false);
            if (trx != null && trx.getConnection() != null) {
                try (java.sql.Statement stmt = trx.getConnection().createStatement()) {
                    stmt.execute("SELECT 1");
                    SetupLog.log("CoA交易檢查", step + ": OK");
                }
            }
        } catch (java.sql.SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("aborted")) {
                SetupLog.log("CoA交易異常", step + ": ABORTED! " + msg);
            } else {
                SetupLog.log("CoA交易錯誤", step + ": " + msg);
            }
        } catch (Exception e) {
            SetupLog.log("CoA交易檢查失敗", step + ": " + e.getMessage());
        }
    }
}
