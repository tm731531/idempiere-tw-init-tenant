/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.util;

import java.util.logging.Level;

import org.compiere.util.CLogger;

/**
 * 設定日誌工具類別
 * <p>
 * 使用 iDempiere 標準的 CLogger 記錄日誌。
 * </p>
 */
public class SetupLog {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SetupLog.class);

    /** 日誌前綴 */
    private static final String PREFIX = "[TW-Sample] ";

    /**
     * 記錄一般訊息
     *
     * @param step 步驟名稱
     * @param message 訊息內容
     */
    public static void log(String step, String message) {
        String fullMessage = PREFIX + step + ": " + message;
        log.info(fullMessage);
    }

    /**
     * 記錄錯誤訊息
     *
     * @param step 步驟名稱
     * @param message 訊息內容
     * @param e 例外物件（可為 null）
     */
    public static void logError(String step, String message, Exception e) {
        String fullMessage = PREFIX + "ERROR: " + step + ": " + message;

        if (e != null) {
            log.log(Level.SEVERE, fullMessage, e);
        } else {
            log.severe(fullMessage);
        }
    }

    /**
     * 記錄警告訊息
     *
     * @param step 步驟名稱
     * @param message 訊息內容
     */
    public static void logWarning(String step, String message) {
        String fullMessage = PREFIX + step + ": " + message;
        log.warning(fullMessage);
    }

    /**
     * 記錄除錯訊息（細節）
     *
     * @param step 步驟名稱
     * @param message 訊息內容
     */
    public static void logFine(String step, String message) {
        String fullMessage = PREFIX + step + ": " + message;
        log.fine(fullMessage);
    }

    /**
     * 清除舊的日誌記錄（保留方法簽名以維持相容性，但不再執行任何操作）
     */
    public static void clearOldLogs() {
        // 不再需要清除，CLogger 自行管理
    }
}
