/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample;

import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import tw.idempiere.sample.cleanup.SampleClientCleanup;

/**
 * 天地人實業示範資料插件 - OSGi Activator
 * <p>
 * 管理插件的生命週期，負責示範資料的建立與清理。
 * </p>
 * <p>
 * 生命週期：
 * <ul>
 *   <li>start(): 由 DS 元件 SampleDataInitializer 負責建立示範資料</li>
 *   <li>stop(): 清理所有示範資料</li>
 * </ul>
 * </p>
 * <p>
 * 注意：由於 OSGi 的限制，無法在 UNINSTALLED 事件時執行 bundle 內的類別，
 * 因此改為在 stop() 時執行清理。配合 start() 時的冪等建立邏輯（skip-if-exists），
 * 重新 start 時會自動重建資料。
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class Activator implements BundleActivator {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(Activator.class);

    /** 示範 Client 的 Search Key，用於檢查是否已存在 */
    public static final String CLIENT_VALUE = "sample";

    /**
     * 插件啟動
     * <p>
     * 初始化由 DS 元件 SampleDataInitializer 負責，避免雙重初始化競爭。
     * </p>
     *
     * @param context OSGi BundleContext
     * @throws Exception 啟動失敗時拋出
     */
    @Override
    public void start(BundleContext context) throws Exception {
        log.info("=== 天地人實業示範資料插件啟動 ===");
        System.out.println("=== 天地人實業示範資料插件啟動 ===");
        // 注意：初始化由 DS 元件 SampleDataInitializer 負責
        // 避免在此處重複執行初始化，防止競爭條件
    }

    /**
     * 插件停止
     * <p>
     * 停止時清理所有示範資料。
     * 下次 start() 時會自動重建（配合 skip-if-exists 邏輯）。
     * </p>
     *
     * @param context OSGi BundleContext
     * @throws Exception 停止失敗時拋出
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("天地人實業示範資料插件停止中，開始清理資料...");
        try {
            SampleClientCleanup.cleanup();
            log.info("示範資料清理完成");
        } catch (Exception e) {
            log.log(Level.SEVERE, "清理示範資料時發生錯誤", e);
        }
    }

    /**
     * 手動執行清理（可從 OSGi console 呼叫）
     */
    public static void manualCleanup() {
        log.info("手動清理示範資料...");
        try {
            SampleClientCleanup.cleanup();
            log.info("手動清理完成");
        } catch (Exception e) {
            log.log(Level.SEVERE, "手動清理時發生錯誤", e);
        }
    }

    /**
     * 手動執行初始化（可從 OSGi console 呼叫）
     * <p>
     * 使用方式：在 OSGi console 執行
     * <pre>
     * osgi> tw.idempiere.sample.Activator manualInit
     * </pre>
     * 或使用 Groovy：
     * <pre>
     * tw.idempiere.sample.Activator.manualInit()
     * </pre>
     * </p>
     */
    public static void manualInit() {
        System.out.println("=== 手動初始化天地人實業示範資料 ===");
        try {
            // 設定 System context
            org.compiere.util.Env.setContext(org.compiere.util.Env.getCtx(), org.compiere.util.Env.AD_CLIENT_ID, 0);
            org.compiere.util.Env.setContext(org.compiere.util.Env.getCtx(), org.compiere.util.Env.AD_ORG_ID, 0);
            org.compiere.util.Env.setContext(org.compiere.util.Env.getCtx(), org.compiere.util.Env.AD_USER_ID, 0);
            org.compiere.util.Env.setContext(org.compiere.util.Env.getCtx(), org.compiere.util.Env.AD_ROLE_ID, 0);

            boolean success = tw.idempiere.sample.setup.SampleClientSetup.init();
            if (success) {
                System.out.println("=== 初始化成功 ===");
            } else {
                System.out.println("=== 初始化失敗或已存在 ===");
            }
        } catch (Exception e) {
            System.out.println("=== 初始化錯誤: " + e.getMessage() + " ===");
            e.printStackTrace();
        }
    }
}
