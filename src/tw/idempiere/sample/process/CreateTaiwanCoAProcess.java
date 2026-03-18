/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.process;

import org.compiere.process.SvrProcess;
import org.compiere.util.Trx;

import tw.idempiere.sample.setup.TaiwanCoASetup;

/**
 * 為當前 Client 建立台灣會計科目的 Process
 * <p>
 * 可從 iDempiere UI 執行，為目前登入的 Client 建立台灣標準會計科目表，
 * 並設定 Accounting Schema Defaults 對應。
 * </p>
 * <p>
 * 使用方式：
 * <ol>
 *   <li>登入要建立會科的 Client（例如 F5）</li>
 *   <li>執行此 Process</li>
 *   <li>台灣會計科目會自動建立並設定 Defaults</li>
 * </ol>
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class CreateTaiwanCoAProcess extends SvrProcess {

    @Override
    protected void prepare() {
        // 無需參數
    }

    @Override
    protected String doIt() throws Exception {
        int clientId = getAD_Client_ID();
        addLog("開始為 Client ID=" + clientId + " 建立台灣會計科目...");

        if (clientId == 0) {
            addLog("錯誤：不可對 System Client 執行此操作");
            return "錯誤：請登入非 System 的 Client";
        }

        String trxName = Trx.createTrxName("TW_CoA");
        Trx trx = Trx.get(trxName, true);

        try {
            boolean success = TaiwanCoASetup.createTaiwanCoA(getCtx(), clientId, trxName);

            if (success) {
                trx.commit();
                addLog("台灣會計科目建立成功！");
                addLog("請重新開啟 Account Element 視窗查看新增的會計科目");
                addLog("請檢查 Accounting Schema > Defaults 確認對應設定");
                return "建立成功";
            } else {
                trx.rollback();
                addLog("建立失敗，請檢查日誌");
                return "建立失敗";
            }
        } catch (Exception e) {
            trx.rollback();
            addLog("錯誤: " + e.getClass().getName() + " - " + e.getMessage());
            if (e.getCause() != null) {
                addLog("原因: " + e.getCause().getMessage());
            }
            throw e;
        } finally {
            trx.close();
        }
    }
}
