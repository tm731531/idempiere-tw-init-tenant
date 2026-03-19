/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.util.Properties;

import org.compiere.model.MBank;
import org.compiere.model.MBankAccount;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import tw.idempiere.sample.util.SetupLog;

/**
 * 銀行帳戶設定
 * <p>
 * 建立示範公司的銀行帳戶：
 * <ul>
 *   <li>台灣銀行 - 台北分行台幣帳戶</li>
 * </ul>
 * </p>
 *
 * @author 天地人實業
 * @version 2.1.0
 */
public class SampleBankAccountSetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleBankAccountSetup.class);

    /** 銀行帳戶 ID */
    private static int bankAccountId = 0;

    /** 私有建構子，防止實例化 */
    private SampleBankAccountSetup() {
        // 工具類別，不需要實例化
    }

    /**
     * 建立銀行和銀行帳戶
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    public static boolean createBankAccounts(Properties ctx, int clientId, String trxName) {
        log.info("開始建立銀行帳戶...");
        SetupLog.log("銀行帳戶", "開始建立銀行帳戶");

        try {
            // 取得台幣 ID
            int currencyId = DB.getSQLValue(trxName,
                "SELECT C_Currency_ID FROM C_Currency WHERE ISO_Code='TWD'");
            if (currencyId <= 0) {
                currencyId = DB.getSQLValue(trxName,
                    "SELECT C_Currency_ID FROM C_Currency WHERE ISO_Code='USD'");
            }

            // 取得總公司組織 ID
            int orgId = DB.getSQLValue(trxName,
                "SELECT AD_Org_ID FROM AD_Org WHERE AD_Client_ID=? AND Value='HQ'",
                clientId);
            if (orgId <= 0) {
                orgId = DB.getSQLValue(trxName,
                    "SELECT MIN(AD_Org_ID) FROM AD_Org WHERE AD_Client_ID=? AND AD_Org_ID > 0",
                    clientId);
            }

            // 步驟 1：檢查或建立銀行
            int bankId = getOrCreateBank(ctx, "台灣銀行", "004", "台北分行", trxName);
            if (bankId <= 0) {
                log.severe("無法建立銀行");
                return false;
            }

            // 步驟 2：檢查銀行帳戶是否已存在
            String accountNo = "004-12345678";
            int existingId = DB.getSQLValue(trxName,
                "SELECT C_BankAccount_ID FROM C_BankAccount WHERE AD_Client_ID=? AND AccountNo=?",
                clientId, accountNo);

            if (existingId > 0) {
                bankAccountId = existingId;
                SetupLog.log("銀行帳戶", "銀行帳戶已存在，ID=" + existingId);
                log.info("銀行帳戶已存在，跳過");
                return true;
            }

            // 步驟 3：建立銀行帳戶
            MBankAccount ba = new MBankAccount(ctx, 0, trxName);
            ba.setAD_Org_ID(orgId);
            ba.setC_Bank_ID(bankId);
            ba.setAccountNo(accountNo);
            ba.setDescription("天地人實業有限公司 台幣帳戶");
            ba.setC_Currency_ID(currencyId);
            ba.setBankAccountType(MBankAccount.BANKACCOUNTTYPE_Checking);
            ba.setIsDefault(true);
            ba.setCurrentBalance(Env.ZERO);

            if (!ba.save()) {
                log.severe("無法儲存銀行帳戶");
                SetupLog.logError("銀行帳戶", "儲存失敗", null);
                return false;
            }

            bankAccountId = ba.getC_BankAccount_ID();
            SetupLog.log("銀行帳戶", "已建立銀行帳戶: " + accountNo + ", ID=" + bankAccountId);
            log.info("銀行帳戶建立完成: " + accountNo);
            return true;

        } catch (Exception e) {
            log.severe("建立銀行帳戶時發生錯誤: " + e.getMessage());
            SetupLog.logError("銀行帳戶", "建立失敗", e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 取得或建立銀行
     *
     * @param ctx 系統上下文
     * @param name 銀行名稱
     * @param routingNo 銀行代碼
     * @param description 說明
     * @param trxName 交易名稱
     * @return 銀行 ID，失敗時回傳 0
     */
    private static int getOrCreateBank(Properties ctx, String name, String routingNo,
            String description, String trxName) {

        // 檢查是否已存在（系統級別，AD_Client_ID=0）
        int existingId = DB.getSQLValue(trxName,
            "SELECT C_Bank_ID FROM C_Bank WHERE RoutingNo=?", routingNo);

        if (existingId > 0) {
            log.fine("銀行已存在: " + name);
            return existingId;
        }

        // 建立新銀行（系統級別）
        MBank bank = new MBank(ctx, 0, trxName);
        bank.setAD_Org_ID(0);
        bank.setName(name);
        bank.setRoutingNo(routingNo);
        bank.setDescription(description);

        // 注意：MBank 沒有 setC_Country_ID，國家由 Location 決定

        if (!bank.save()) {
            log.severe("無法儲存銀行: " + name);
            return 0;
        }

        log.fine("已建立銀行: " + name);
        return bank.getC_Bank_ID();
    }

    /**
     * 取得銀行帳戶 ID
     *
     * @return 銀行帳戶 ID，不存在時回傳 0
     */
    public static int getBankAccountId() {
        return bankAccountId;
    }
}
