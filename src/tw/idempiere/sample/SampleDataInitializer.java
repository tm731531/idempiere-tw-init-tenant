/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample;

import java.util.logging.Level;

import org.compiere.Adempiere;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import tw.idempiere.sample.setup.SampleClientSetup;

/**
 * 示範資料初始化 DS 元件
 * <p>
 * 使用 Declarative Services 在 iDempiere 環境就緒後執行初始化。
 * 這個元件會在 OSGi 容器啟動服務後被激活，此時 iDempiere 環境應該已經準備好。
 * </p>
 * <p>
 * 此元件由 OSGI-INF/component.xml 定義，在 Bundle 啟動時自動激活。
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class SampleDataInitializer {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleDataInitializer.class);

    /** 最大重試次數 */
    private static final int MAX_RETRIES = 10;

    /** 重試間隔（毫秒） */
    private static final long RETRY_INTERVAL = 3000;

    /** 初始化執行緒 */
    private Thread initThread;

    /** 是否已停止 */
    private volatile boolean stopped = false;

    /**
     * DS 元件啟動
     * <p>
     * 在背景執行緒中執行初始化，並等待 iDempiere 環境就緒。
     * 此方法由 OSGi DS 容器呼叫（定義於 OSGI-INF/component.xml）。
     * </p>
     */
    public void activate() {
        log.info("SampleDataInitializer 元件啟動");
        tw.idempiere.sample.util.SetupLog.log("DS啟動", "SampleDataInitializer.activate() 被調用");
        stopped = false;

        // 使用背景執行緒進行初始化，避免阻塞 OSGi 啟動
        initThread = new Thread(() -> {
            tw.idempiere.sample.util.SetupLog.log("執行緒", "背景執行緒開始");
            initializeWithRetry();
        }, "SampleDataInitializer");
        initThread.setDaemon(true);
        initThread.start();
    }

    /**
     * DS 元件停止
     * <p>
     * 此方法由 OSGi DS 容器呼叫（定義於 OSGI-INF/component.xml）。
     * </p>
     */
    public void deactivate() {
        log.info("SampleDataInitializer 元件停止");
        stopped = true;

        // 中斷初始化執行緒
        if (initThread != null && initThread.isAlive()) {
            initThread.interrupt();
        }
    }

    /**
     * 帶重試的初始化
     * <p>
     * 等待 iDempiere 環境就緒，然後執行初始化。
     * 最多重試 MAX_RETRIES 次，每次間隔 RETRY_INTERVAL 毫秒。
     * </p>
     */
    private void initializeWithRetry() {
        log.info("開始等待 iDempiere 環境就緒...");
        tw.idempiere.sample.util.SetupLog.log("重試迴圈", "initializeWithRetry() 開始");

        for (int attempt = 1; attempt <= MAX_RETRIES && !stopped; attempt++) {
            try {
                tw.idempiere.sample.util.SetupLog.log("嘗試", "第 " + attempt + " 次嘗試");
                // 檢查 iDempiere 環境是否就緒
                if (isEnvironmentReady()) {
                    log.info("iDempiere 環境就緒，開始初始化示範資料（嘗試 " + attempt + "/" + MAX_RETRIES + "）");
                    tw.idempiere.sample.util.SetupLog.log("環境就緒", "準備調用 SampleClientSetup.init()");

                    boolean success = SampleClientSetup.init();
                    tw.idempiere.sample.util.SetupLog.log("init結果", "success=" + success);
                    if (success) {
                        log.info("天地人實業示範資料初始化完成");
                    } else {
                        log.warning("天地人實業示範資料初始化失敗（可能已存在）");
                    }
                    return; // 成功或已存在，結束
                }

                log.info("iDempiere 環境尚未就緒，等待中...（嘗試 " + attempt + "/" + MAX_RETRIES + "）");
                Thread.sleep(RETRY_INTERVAL);

            } catch (InterruptedException e) {
                log.info("初始化被中斷");
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.log(Level.WARNING, "初始化嘗試 " + attempt + " 失敗: " + e.getMessage(), e);
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_INTERVAL);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        if (!stopped) {
            log.severe("已達最大重試次數，示範資料初始化失敗。請手動執行初始化。");
        }
    }

    /**
     * 檢查 iDempiere 環境是否就緒
     * <p>
     * 檢查項目：
     * <ul>
     *   <li>Adempiere.isStarted() 回傳 true</li>
     *   <li>資料庫連線可用</li>
     * </ul>
     * </p>
     *
     * @return true=環境就緒, false=尚未就緒
     */
    private boolean isEnvironmentReady() {
        try {
            // 檢查 Adempiere 是否已啟動
            if (!Adempiere.isStarted()) {
                log.fine("Adempiere 尚未啟動");
                return false;
            }

            // 檢查資料庫連線（使用 isConnected 而非已棄用的 getConnectionRO）
            if (!org.compiere.util.DB.isConnected()) {
                log.fine("資料庫連線尚未就緒");
                return false;
            }

            // 設定系統 context（使用 System 用戶）
            Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, 0);
            Env.setContext(Env.getCtx(), Env.AD_ORG_ID, 0);
            Env.setContext(Env.getCtx(), Env.AD_USER_ID, 0); // System

            return true;

        } catch (Exception e) {
            log.fine("環境檢查時發生錯誤: " + e.getMessage());
            return false;
        }
    }
}
