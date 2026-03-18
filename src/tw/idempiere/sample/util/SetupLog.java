/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.util;

import org.compiere.util.DB;

/**
 * 設定日誌工具類別
 * <p>
 * 將日誌寫入 AD_Note 表格，方便在 WebUI 或資料庫中查看。
 * </p>
 */
public class SetupLog {

    private static final int SYSTEM_USER_ID = 0;
    private static final int SYSTEM_CLIENT_ID = 0;

    /**
     * 記錄訊息到 AD_Note
     */
    public static void log(String step, String message) {
        log(step, message, null);
    }

    /**
     * 記錄錯誤到 AD_Note
     */
    public static void logError(String step, String message, Exception e) {
        String fullMessage = message;
        if (e != null) {
            fullMessage += "\n錯誤類型: " + e.getClass().getName();
            fullMessage += "\n錯誤訊息: " + e.getMessage();

            // 取得 stack trace 前幾行
            StackTraceElement[] stack = e.getStackTrace();
            if (stack != null && stack.length > 0) {
                fullMessage += "\nStack Trace:";
                for (int i = 0; i < Math.min(5, stack.length); i++) {
                    fullMessage += "\n  " + stack[i].toString();
                }
            }
        }
        log(step, fullMessage, "E");
    }

    /**
     * 記錄到自訂表格 tw_sample_log
     * @param step 步驟名稱
     * @param message 訊息內容
     * @param type 類型：null=INFO, E=ERROR
     */
    private static void log(String step, String message, String type) {
        try {
            String fullStep = step;
            if (type != null && type.equals("E")) {
                fullStep = "ERROR: " + step;
            }

            // 確保表格存在
            ensureLogTableExists();

            // 使用簡單的自訂表格記錄
            String sql = "INSERT INTO tw_sample_log (step, message) VALUES (?, ?)";
            DB.executeUpdate(sql, new Object[]{fullStep, message}, false, null);

            // 同時輸出到 System.out
            System.out.println("[TW-Sample] " + fullStep + ": " + message);

        } catch (Exception ex) {
            // 如果寫入失敗，至少輸出到 System.out
            System.out.println("[TW-Sample LOG ERROR] " + step + ": " + message + " | Error: " + ex.getMessage());
        }
    }

    /**
     * 確保日誌表格存在
     */
    private static void ensureLogTableExists() {
        try {
            String sql = "CREATE TABLE IF NOT EXISTS tw_sample_log (" +
                "id SERIAL PRIMARY KEY, " +
                "created TIMESTAMP DEFAULT now(), " +
                "step VARCHAR(100), " +
                "message TEXT)";
            DB.executeUpdate(sql, null, false, null);
        } catch (Exception e) {
            // 忽略（表格可能已存在）
        }
    }

    /**
     * 清除舊的日誌記錄
     */
    public static void clearOldLogs() {
        try {
            String sql = "DELETE FROM AD_Note WHERE Reference LIKE '[TW-Sample%' AND Created < now() - interval '1 day'";
            DB.executeUpdate(sql, null, false, null);
        } catch (Exception e) {
            // 忽略錯誤
        }
    }
}
