/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MProcess;
import org.compiere.model.MPInstance;
import org.compiere.model.X_I_ElementValue;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.ServerProcessCtl;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;

import tw.idempiere.sample.util.SetupLog;

/**
 * 台灣會計科目表建立類別
 * <p>
 * 使用 iDempiere Import 機制匯入會計科目，確保階層關係正確建立。
 * CSV 檔案位於 data/Accounting_tw.csv
 * </p>
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
 *
 * @author 天地人實業
 * @version 2.0 - 改用 Import 機制
 */
public class TaiwanCoASetup {

    private static final CLogger log = CLogger.getCLogger(TaiwanCoASetup.class);

    /** CSV 檔案路徑（相對於 bundle） */
    private static final String CSV_PATH = "/data/Accounting_tw.csv";

    /**
     * 建立台灣會計科目表（使用 Import 機制）
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱（此方法會建立獨立交易）
     * @return true=成功
     */
    public static boolean createTaiwanCoA(Properties ctx, int clientId, String trxName) {
        log.info("開始建立台灣會計科目表（Import 機制）...");
        SetupLog.log("TaiwanCoA", "開始建立，Client=" + clientId + ", 使用 Import 機制");
        SetupLog.log("CoA版本", "=== 版本 20260319-v4 (Import 機制) ===");

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

            // 讀取 CSV 並匯入
            boolean success = importAccountsFromCSV(ctx, clientId, elementId);

            if (success) {
                log.info("台灣會計科目表建立完成（Import 機制）");
                SetupLog.log("TaiwanCoA", "會計科目匯入成功");
            } else {
                log.severe("台灣會計科目表建立失敗");
                SetupLog.log("TaiwanCoA錯誤", "會計科目匯入失敗");
            }

            return success;

        } catch (Exception e) {
            log.severe("建立台灣會計科目表時發生錯誤: " + e.getMessage());
            SetupLog.logError("TaiwanCoA錯誤", "建立時發生錯誤", e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 從 CSV 檔案匯入會計科目
     * <p>
     * 步驟：
     * 1. 讀取 CSV 檔案
     * 2. 將資料插入 I_ElementValue 暫存表
     * 3. 執行 ImportAccount 流程
     * </p>
     */
    private static boolean importAccountsFromCSV(Properties ctx, int clientId, int elementId) {
        SetupLog.log("Import", "開始從 CSV 匯入會計科目...");

        // 使用獨立交易避免與 MSetup 衝突
        String importTrxName = Trx.createTrxName("ImportCoA_" + System.currentTimeMillis());
        Trx importTrx = Trx.get(importTrxName, true);

        try {
            // 步驟 1：清空舊的未匯入資料
            int deleted = DB.executeUpdate(
                "DELETE FROM I_ElementValue WHERE I_IsImported<>'Y' AND AD_Client_ID=?",
                new Object[]{clientId}, false, importTrxName);
            SetupLog.log("Import", "清空舊資料: " + deleted + " 筆");

            // 步驟 2：讀取 CSV 並插入 I_ElementValue
            List<AccountData> accounts = readCSV();
            if (accounts.isEmpty()) {
                SetupLog.log("Import錯誤", "CSV 檔案為空或讀取失敗");
                return false;
            }
            SetupLog.log("Import", "讀取到 " + accounts.size() + " 筆會計科目");

            int inserted = 0;
            // 設定 Context 中的 Client ID
            Properties importCtx = new Properties();
            importCtx.putAll(ctx);
            Env.setContext(importCtx, Env.AD_CLIENT_ID, clientId);
            Env.setContext(importCtx, Env.AD_ORG_ID, 0);

            for (AccountData acct : accounts) {
                try {
                    X_I_ElementValue imp = new X_I_ElementValue(importCtx, 0, importTrxName);
                    imp.setC_Element_ID(elementId);
                    imp.setValue(acct.value);
                    imp.setName(acct.name);
                    imp.setDescription(acct.description);
                    imp.setAccountType(acct.accountType);
                    imp.setAccountSign(acct.accountSign);
                    imp.setIsSummary(acct.isSummary);
                    imp.setIsDocControlled(acct.isDocControlled);
                    imp.setPostActual(true);
                    imp.setPostBudget(true);
                    imp.setPostStatistical(true);
                    imp.setPostEncumbrance(true);

                    // 設定父科目（階層關鍵）
                    if (acct.parentValue != null && !acct.parentValue.isEmpty()) {
                        imp.setParentValue(acct.parentValue);
                    }

                    // 設定 Default Account
                    if (acct.defaultAccount != null && !acct.defaultAccount.isEmpty()) {
                        imp.setDefault_Account(acct.defaultAccount);
                    }

                    imp.setI_IsImported(false);
                    imp.saveEx();
                    inserted++;
                } catch (Exception e) {
                    SetupLog.logError("Import", "插入失敗: " + acct.value, e);
                }
            }
            SetupLog.log("Import", "插入 I_ElementValue: " + inserted + " 筆");

            // 步驟 3：執行 ImportAccount 流程
            boolean importSuccess = runImportAccountProcess(ctx, clientId, elementId, importTrxName);

            if (importSuccess) {
                importTrx.commit();
                SetupLog.log("Import", "ImportAccount 流程執行成功，交易已提交");

                // 驗證結果
                verifyImportResult(clientId, importTrxName);
                return true;
            } else {
                importTrx.rollback();
                SetupLog.log("Import錯誤", "ImportAccount 流程執行失敗，交易已回滾");
                return false;
            }

        } catch (Exception e) {
            SetupLog.logError("Import錯誤", "匯入過程發生錯誤", e);
            importTrx.rollback();
            return false;
        } finally {
            importTrx.close();
        }
    }

    /**
     * 讀取 CSV 檔案
     * CSV 格式：[Account_Value],[Account_Name],[Account_Description],[Account_Type],
     *          [Account_Sign],[Account_Document],[Account_Summary],[Default_Account],[Account_Parent]
     */
    private static List<AccountData> readCSV() {
        List<AccountData> accounts = new ArrayList<>();

        try {
            // 從 bundle 資源讀取
            InputStream is = TaiwanCoASetup.class.getResourceAsStream(CSV_PATH);
            if (is == null) {
                SetupLog.log("CSV錯誤", "找不到檔案: " + CSV_PATH);
                // 嘗試備用路徑
                is = TaiwanCoASetup.class.getClassLoader().getResourceAsStream("data/Accounting_tw.csv");
            }
            if (is == null) {
                SetupLog.log("CSV錯誤", "備用路徑也找不到檔案");
                return accounts;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {

                String line;
                boolean isHeader = true;
                int lineNum = 0;

                while ((line = reader.readLine()) != null) {
                    lineNum++;

                    // 跳過標題行
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }

                    // 跳過空行
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        AccountData acct = parseCSVLine(line);
                        if (acct != null) {
                            accounts.add(acct);
                        }
                    } catch (Exception e) {
                        SetupLog.log("CSV解析錯誤", "行 " + lineNum + ": " + e.getMessage());
                    }
                }
            }

            SetupLog.log("CSV", "成功讀取 " + accounts.size() + " 筆資料");

        } catch (Exception e) {
            SetupLog.logError("CSV錯誤", "讀取檔案失敗", e);
        }

        return accounts;
    }

    /**
     * 解析 CSV 行
     * 格式：Value,Name,Description,Type,Sign,Document,Summary,DefaultAccount,Parent
     */
    private static AccountData parseCSVLine(String line) {
        // 簡單的 CSV 解析（不處理引號內的逗號）
        String[] parts = line.split(",", -1);

        if (parts.length < 2) {
            return null;
        }

        AccountData acct = new AccountData();
        acct.value = parts[0].trim();
        acct.name = parts.length > 1 ? parts[1].trim() : "";
        acct.description = parts.length > 2 ? parts[2].trim() : "";

        // Account Type: A=Asset, E=Expense, L=Liability, O=Owner's Equity, R=Revenue
        String typeStr = parts.length > 3 ? parts[3].trim() : "E";
        acct.accountType = convertAccountType(typeStr);

        // Account Sign: N=Natural, D=Debit, C=Credit
        String signStr = parts.length > 4 ? parts[4].trim() : "";
        acct.accountSign = convertAccountSign(signStr);

        // Is Document Controlled
        String docStr = parts.length > 5 ? parts[5].trim() : "";
        acct.isDocControlled = "Yes".equalsIgnoreCase(docStr) || "Y".equalsIgnoreCase(docStr);

        // Is Summary
        String summaryStr = parts.length > 6 ? parts[6].trim() : "";
        acct.isSummary = "Yes".equalsIgnoreCase(summaryStr) || "Y".equalsIgnoreCase(summaryStr);

        // Default Account
        acct.defaultAccount = parts.length > 7 ? parts[7].trim() : "";

        // Parent Value（階層關鍵）
        acct.parentValue = parts.length > 8 ? parts[8].trim() : "";

        return acct;
    }

    /**
     * 轉換 Account Type
     */
    private static String convertAccountType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) return "E";

        String upper = typeStr.toUpperCase();
        if (upper.startsWith("A")) return "A";  // Asset
        if (upper.startsWith("L")) return "L";  // Liability
        if (upper.startsWith("O")) return "O";  // Owner's Equity
        if (upper.startsWith("R")) return "R";  // Revenue
        if (upper.startsWith("E")) return "E";  // Expense
        if (upper.startsWith("M")) return "M";  // Memo

        return "E";  // 預設為 Expense
    }

    /**
     * 轉換 Account Sign
     */
    private static String convertAccountSign(String signStr) {
        if (signStr == null || signStr.isEmpty()) return "N";

        String upper = signStr.toUpperCase();
        if (upper.startsWith("D")) return "D";  // Debit
        if (upper.startsWith("C")) return "C";  // Credit

        return "N";  // Natural
    }

    /**
     * 執行 ImportAccount 流程
     */
    private static boolean runImportAccountProcess(Properties ctx, int clientId,
            int elementId, String trxName) {

        SetupLog.log("ImportProcess", "開始執行 ImportAccount 流程...");

        try {
            // 取得 ImportAccount 流程 ID
            int processId = MProcess.getProcess_ID("ImportAccount", trxName);
            if (processId <= 0) {
                // 嘗試用 Value 查詢
                processId = DB.getSQLValue(trxName,
                    "SELECT AD_Process_ID FROM AD_Process WHERE Value='ImportAccount'");
            }
            if (processId <= 0) {
                // 備用：直接用已知的 Process ID（通常是 189）
                processId = DB.getSQLValue(trxName,
                    "SELECT AD_Process_ID FROM AD_Process WHERE Name='Import Account'");
            }

            SetupLog.log("ImportProcess", "Process ID = " + processId);

            if (processId <= 0) {
                SetupLog.log("ImportProcess錯誤", "找不到 ImportAccount 流程");
                return false;
            }

            // 建立 Process Instance（使用帶有正確 Client ID 的 context）
            Properties procCtx = new Properties();
            procCtx.putAll(ctx);
            Env.setContext(procCtx, Env.AD_CLIENT_ID, clientId);
            Env.setContext(procCtx, Env.AD_ORG_ID, 0);

            MPInstance instance = new MPInstance(procCtx, processId, 0);
            instance.saveEx();
            int pInstanceId = instance.getAD_PInstance_ID();
            SetupLog.log("ImportProcess", "PInstance ID = " + pInstanceId);

            // 建立 ProcessInfo
            ProcessInfo pi = new ProcessInfo("Import Account", processId);
            pi.setAD_Client_ID(clientId);
            pi.setAD_User_ID(0);
            pi.setAD_PInstance_ID(pInstanceId);

            // 設定流程參數
            ProcessInfoParameter[] params = new ProcessInfoParameter[] {
                new ProcessInfoParameter("AD_Client_ID", new BigDecimal(clientId), null, null, null),
                new ProcessInfoParameter("C_Element_ID", new BigDecimal(elementId), null, null, null),
                new ProcessInfoParameter("UpdateDefaultAccounts", "Y", null, null, null),
                new ProcessInfoParameter("CreateNewCombination", "Y", null, null, null),
                new ProcessInfoParameter("DeleteOldImported", "N", null, null, null)
            };
            pi.setParameter(params);

            // 儲存參數到資料庫
            for (ProcessInfoParameter param : params) {
                // 插入 AD_PInstance_Para
                String sql = "INSERT INTO AD_PInstance_Para " +
                    "(AD_PInstance_ID, SeqNo, ParameterName, P_String, P_Number, " +
                    "AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 0, 'Y', NOW(), 0, NOW(), 0)";

                Object pValue = param.getParameter();
                String pString = null;
                BigDecimal pNumber = null;

                if (pValue instanceof String) {
                    pString = (String) pValue;
                } else if (pValue instanceof BigDecimal) {
                    pNumber = (BigDecimal) pValue;
                }

                DB.executeUpdate(sql,
                    new Object[]{pInstanceId, params.length, param.getParameterName(),
                        pString, pNumber, clientId},
                    false, trxName);
            }

            // 執行流程
            Trx trx = Trx.get(trxName, false);
            ServerProcessCtl processCtl = new ServerProcessCtl(pi, trx);
            processCtl.run();

            // 檢查結果
            if (pi.isError()) {
                SetupLog.log("ImportProcess錯誤", "執行失敗: " + pi.getSummary());
                return false;
            }

            SetupLog.log("ImportProcess", "執行成功: " + pi.getSummary());
            return true;

        } catch (Exception e) {
            SetupLog.logError("ImportProcess錯誤", "執行流程時發生錯誤", e);
            return false;
        }
    }

    /**
     * 驗證匯入結果
     */
    private static void verifyImportResult(int clientId, String trxName) {
        try {
            // 檢查匯入的科目數量
            int importedCount = DB.getSQLValue(null,
                "SELECT COUNT(*) FROM I_ElementValue WHERE AD_Client_ID=? AND I_IsImported='Y'",
                clientId);

            // 檢查 C_ElementValue 中的科目數量
            int evCount = DB.getSQLValue(null,
                "SELECT COUNT(*) FROM C_ElementValue WHERE AD_Client_ID=? AND Value ~ '^[1-8]'",
                clientId);

            // 檢查有階層關係的科目數量
            int treeCount = DB.getSQLValue(null,
                "SELECT COUNT(*) FROM AD_TreeNode tn " +
                "JOIN C_ElementValue ev ON tn.Node_ID = ev.C_ElementValue_ID " +
                "WHERE ev.AD_Client_ID=? AND tn.Parent_ID > 0",
                clientId);

            SetupLog.log("Import驗證",
                "I_ElementValue已匯入: " + importedCount +
                ", C_ElementValue: " + evCount +
                ", 有階層: " + treeCount);

        } catch (Exception e) {
            SetupLog.logError("Import驗證錯誤", "驗證時發生錯誤", e);
        }
    }

    /**
     * 會計科目資料結構
     */
    private static class AccountData {
        String value;
        String name;
        String description;
        String accountType;
        String accountSign;
        boolean isDocControlled;
        boolean isSummary;
        String defaultAccount;
        String parentValue;
    }

    /**
     * 在主交易提交後更新 Accounting Schema Defaults
     * （保留舊方法以供向後相容）
     */
    public static boolean updateAcctSchemaDefaultsPostCommit(Properties ctx, int clientId) {
        SetupLog.log("AcctDefaults", "使用 Import 機制，Default Accounts 已由 ImportAccount 處理");
        return true;
    }
}
