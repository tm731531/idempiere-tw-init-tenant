/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.MInventory;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MLocator;
import org.compiere.model.MProduct;
import org.compiere.model.MWarehouse;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import tw.idempiere.sample.util.SetupLog;

/**
 * 初始庫存設定
 * <p>
 * 建立盤點單設定初始庫存，讓使用者可以立即測試銷售流程。
 * </p>
 *
 * @author 天地人實業
 * @version 2.1.0
 */
public class SampleInventorySetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleInventorySetup.class);

    /**
     * 初始庫存資料定義
     * <p>
     * 欄位順序：{商品代碼, 數量}
     * </p>
     */
    private static final Object[][] INITIAL_INVENTORY = {
        // 紙類
        {"PAPER-A4-500",  new BigDecimal("100")},  // A4 影印紙 100 包
        {"PAPER-A4-BOX",  new BigDecimal("20")},   // A4 影印紙箱裝 20 箱
        {"NOTEBOOK-25K",  new BigDecimal("50")},   // 筆記本 50 本
        {"NOTEBOOK-18K",  new BigDecimal("30")},   // 筆記本 30 本

        // 書寫工具
        {"PEN-BLUE",      new BigDecimal("200")},  // 原子筆-藍 200 支
        {"PEN-BLACK",     new BigDecimal("200")},  // 原子筆-黑 200 支
        {"PEN-RED",       new BigDecimal("100")},  // 原子筆-紅 100 支
        {"PENCIL-HB",     new BigDecimal("150")},  // 鉛筆 150 支
        {"ERASER",        new BigDecimal("100")},  // 橡皮擦 100 個

        // 辦公用品
        {"CLIP-BOX",      new BigDecimal("50")},   // 迴紋針 50 盒
        {"STAPLER",       new BigDecimal("30")},   // 釘書機 30 台
        {"STAPLE-BOX",    new BigDecimal("100")},  // 釘書針 100 盒
        {"SCISSORS",      new BigDecimal("40")},   // 剪刀 40 把
        {"TAPE-DISPENSER",new BigDecimal("25")},   // 膠帶台 25 台
        {"TAPE-CLEAR",    new BigDecimal("80")},   // 透明膠帶 80 捲

        // 檔案用品
        {"FOLDER-A4",     new BigDecimal("200")},  // 資料夾 200 個

        // 一般商品
        {"ITEM-A",        new BigDecimal("50")},   // 商品 A 50 個
        {"ITEM-B",        new BigDecimal("40")},   // 商品 B 40 個
        {"ITEM-C",        new BigDecimal("60")},   // 商品 C 60 個
        {"ITEM-D",        new BigDecimal("35")}    // 商品 D 35 個
    };

    /** 私有建構子，防止實例化 */
    private SampleInventorySetup() {
        // 工具類別，不需要實例化
    }

    /**
     * 建立初始庫存
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    public static boolean createInitialInventory(Properties ctx, int clientId, String trxName) {
        log.info("開始建立初始庫存...");
        SetupLog.log("初始庫存", "開始建立初始庫存");

        try {
            // 檢查是否已有庫存資料（避免重複建立）
            int existingStock = DB.getSQLValue(trxName,
                "SELECT COUNT(*) FROM M_StorageOnHand WHERE AD_Client_ID=? AND QtyOnHand > 0",
                clientId);

            if (existingStock > 0) {
                SetupLog.log("初始庫存", "已有庫存資料，跳過");
                log.info("已有庫存資料，跳過初始庫存建立");
                return true;
            }

            // 取得台北主倉
            int warehouseId = DB.getSQLValue(trxName,
                "SELECT M_Warehouse_ID FROM M_Warehouse WHERE AD_Client_ID=? AND Value='WH-TP'",
                clientId);

            if (warehouseId <= 0) {
                // 嘗試取得任何可用倉庫
                warehouseId = DB.getSQLValue(trxName,
                    "SELECT MIN(M_Warehouse_ID) FROM M_Warehouse WHERE AD_Client_ID=? AND IsActive='Y'",
                    clientId);
            }

            if (warehouseId <= 0) {
                log.severe("找不到可用倉庫");
                SetupLog.logError("初始庫存", "找不到可用倉庫", null);
                return false;
            }

            MWarehouse wh = new MWarehouse(ctx, warehouseId, trxName);
            int orgId = wh.getAD_Org_ID();

            // 取得預設庫位
            MLocator locator = wh.getDefaultLocator();
            if (locator == null) {
                log.severe("找不到預設庫位");
                SetupLog.logError("初始庫存", "找不到預設庫位", null);
                return false;
            }

            // 建立盤點單
            MInventory inventory = new MInventory(ctx, 0, trxName);
            inventory.setAD_Org_ID(orgId);
            inventory.setM_Warehouse_ID(warehouseId);
            inventory.setDescription("初始庫存設定 - 台灣示範資料");
            inventory.setMovementDate(new Timestamp(System.currentTimeMillis()));

            // 取得實體盤點單據類型（NOT NULL）
            int docTypeId = DB.getSQLValue(trxName,
                "SELECT C_DocType_ID FROM C_DocType WHERE AD_Client_ID=? AND DocBaseType='MMI' AND IsActive='Y'",
                clientId);
            if (docTypeId <= 0) {
                log.severe("找不到盤點單據類型 (MMI)");
                SetupLog.logError("初始庫存", "找不到盤點單據類型", null);
                return false;
            }
            inventory.setC_DocType_ID(docTypeId);

            if (!inventory.save()) {
                log.severe("無法建立盤點單");
                SetupLog.logError("初始庫存", "無法建立盤點單", null);
                return false;
            }

            // 建立盤點明細
            int lineNo = 10;
            int created = 0;

            for (Object[] data : INITIAL_INVENTORY) {
                String productValue = (String) data[0];
                BigDecimal qty = (BigDecimal) data[1];

                // 取得商品 ID
                int productId = DB.getSQLValue(trxName,
                    "SELECT M_Product_ID FROM M_Product WHERE AD_Client_ID=? AND Value=?",
                    clientId, productValue);

                if (productId <= 0) {
                    log.fine("找不到商品: " + productValue + "，跳過");
                    continue;
                }

                MProduct product = new MProduct(ctx, productId, trxName);

                // 建立盤點明細
                MInventoryLine line = new MInventoryLine(ctx, 0, trxName);
                line.setM_Inventory_ID(inventory.getM_Inventory_ID());
                line.setAD_Org_ID(orgId);
                line.setM_Locator_ID(locator.getM_Locator_ID());
                line.setM_Product_ID(productId);
                line.setM_AttributeSetInstance_ID(0);  // 無屬性集實例（AD_Column 標記必填）
                line.setLine(lineNo);

                // 設定數量：QtyBook=0（帳面數量）, QtyCount=實盤數量
                line.setQtyBook(Env.ZERO);
                line.setQtyCount(qty);

                if (!line.save()) {
                    log.warning("無法建立盤點明細: " + productValue);
                    continue;
                }

                lineNo += 10;
                created++;
            }

            SetupLog.log("初始庫存", "已建立 " + created + " 筆盤點明細");

            // 完成盤點單
            if (!inventory.processIt(DocAction.ACTION_Complete)) {
                log.warning("無法完成盤點單: " + inventory.getProcessMsg());
                SetupLog.logWarning("初始庫存", "無法完成盤點單: " + inventory.getProcessMsg());
                // 不回傳失敗，因為盤點單已建立，可以手動完成
            } else {
                inventory.saveEx();
                SetupLog.log("初始庫存", "盤點單已完成，單號: " + inventory.getDocumentNo());
            }

            log.info("初始庫存建立完成，共 " + created + " 筆");
            return true;

        } catch (Exception e) {
            log.severe("建立初始庫存時發生錯誤: " + e.getMessage());
            SetupLog.logError("初始庫存", "建立失敗", e);
            e.printStackTrace();
            return false;
        }
    }
}
