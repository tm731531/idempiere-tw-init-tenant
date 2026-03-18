/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.process;

import java.io.File;
import java.io.InputStream;

import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import tw.idempiere.sample.setup.SampleClientSetup;

/**
 * 建立天地人實業示範資料的 Process
 * <p>
 * 可從 iDempiere UI 執行，手動建立示範 Client 和資料。
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class SampleClientProcess extends SvrProcess {

    @Override
    protected void prepare() {
        // 無需參數
    }

    @Override
    protected String doIt() throws Exception {
        addLog("開始建立天地人實業示範資料...");

        // 測試資源讀取
        addLog("測試資源讀取...");
        try {
            InputStream is1 = getClass().getClassLoader().getResourceAsStream("data/AccountingTW.csv");
            addLog("ClassLoader data/AccountingTW.csv: " + (is1 != null ? "找到" : "找不到"));
            if (is1 != null) is1.close();

            InputStream is2 = getClass().getResourceAsStream("/data/AccountingTW.csv");
            addLog("Class /data/AccountingTW.csv: " + (is2 != null ? "找到" : "找不到"));
            if (is2 != null) is2.close();
        } catch (Exception e) {
            addLog("資源測試錯誤: " + e.getMessage());
        }

        // 設定 System context
        Env.setContext(getCtx(), Env.AD_CLIENT_ID, 0);
        Env.setContext(getCtx(), Env.AD_ORG_ID, 0);
        Env.setContext(getCtx(), Env.AD_USER_ID, 0);
        Env.setContext(getCtx(), Env.AD_ROLE_ID, 0);

        try {
            boolean success = SampleClientSetup.init();

            if (success) {
                addLog("天地人實業示範資料建立成功！");
                return "建立成功";
            } else {
                addLog("建立失敗或已存在");
                return "建立失敗或已存在";
            }
        } catch (Exception e) {
            addLog("錯誤: " + e.getClass().getName() + " - " + e.getMessage());
            if (e.getCause() != null) {
                addLog("原因: " + e.getCause().getMessage());
            }
            throw e;
        }
    }
}
