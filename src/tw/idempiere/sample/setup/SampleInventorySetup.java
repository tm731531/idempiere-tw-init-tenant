/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.util.Properties;

import org.compiere.util.CLogger;

import tw.idempiere.sample.util.SetupLog;

/**
 * 初始庫存設定
 * <p>
 * 注意：由於 iDempiere 核心的 MInventoryLine.beforeSave() 會強制呼叫成本計算，
 * 而新建立的 Client 成本元素設定通常不完整，會導致 NullPointerException。
 * 這是 iDempiere 核心問題，無法從插件層繞過。
 * 因此目前跳過自動建立初始庫存，使用者需在系統設定完成後手動建立盤點單。
 * </p>
 *
 * @author 天地人實業
 * @version 2.1.8
 */
public class SampleInventorySetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleInventorySetup.class);

    /** 私有建構子，防止實例化 */
    private SampleInventorySetup() {
        // 工具類別，不需要實例化
    }

    /**
     * 建立初始庫存
     * <p>
     * 目前此方法會直接跳過，因為 iDempiere 核心的 MInventoryLine.beforeSave()
     * 會強制呼叫 MCostElement.getCostingMethod()，而新建立的 Client 成本元素
     * 設定通常不完整，導致 NullPointerException。
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功（跳過）
     */
    public static boolean createInitialInventory(Properties ctx, int clientId, String trxName) {
        log.info("初始庫存設定...");

        // 由於 iDempiere 核心限制，跳過自動建立初始庫存
        // 使用者需在系統設定完成後，透過「Physical Inventory」視窗手動建立盤點單
        SetupLog.log("初始庫存", "跳過自動建立（需手動建立盤點單以設定初始庫存）");
        log.info("跳過初始庫存自動建立，使用者需手動建立盤點單");

        return true;
    }
}
