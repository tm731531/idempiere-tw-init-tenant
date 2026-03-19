/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import tw.idempiere.sample.util.SetupLog;

/**
 * 示範交易設定
 * <p>
 * 建立示範用的採購單和銷售訂單，讓使用者可以體驗完整的 ERP 流程。
 * 單據以草稿狀態建立，使用者可以自行完成。
 * </p>
 *
 * @author 天地人實業
 * @version 2.1.0
 */
public class SampleTransactionSetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleTransactionSetup.class);

    /** 私有建構子，防止實例化 */
    private SampleTransactionSetup() {
        // 工具類別，不需要實例化
    }

    /**
     * 建立示範交易
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    public static boolean createSampleTransactions(Properties ctx, int clientId, String trxName) {
        log.info("開始建立示範交易...");
        SetupLog.log("示範交易", "開始建立示範交易");

        try {
            // 檢查是否已有訂單（避免重複建立）
            int existingOrders = DB.getSQLValue(trxName,
                "SELECT COUNT(*) FROM C_Order WHERE AD_Client_ID=?", clientId);

            if (existingOrders > 0) {
                SetupLog.log("示範交易", "已有訂單資料，跳過");
                log.info("已有訂單資料，跳過示範交易建立");
                return true;
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

            // 取得倉庫 ID
            int warehouseId = DB.getSQLValue(trxName,
                "SELECT M_Warehouse_ID FROM M_Warehouse WHERE AD_Client_ID=? AND AD_Org_ID=?",
                clientId, orgId);

            int created = 0;

            // 建立採購單
            if (createPurchaseOrder(ctx, clientId, orgId, warehouseId, trxName)) {
                created++;
            }

            // 建立銷售訂單
            if (createSalesOrder(ctx, clientId, orgId, warehouseId, trxName)) {
                created++;
            }

            SetupLog.log("示範交易", "完成，共建立 " + created + " 筆訂單");
            log.info("示範交易建立完成，共 " + created + " 筆");
            return true;

        } catch (Exception e) {
            log.severe("建立示範交易時發生錯誤: " + e.getMessage());
            SetupLog.logError("示範交易", "建立失敗", e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 建立採購單
     */
    private static boolean createPurchaseOrder(Properties ctx, int clientId, int orgId,
            int warehouseId, String trxName) {

        try {
            // 取得供應商
            int bpartnerId = DB.getSQLValue(trxName,
                "SELECT C_BPartner_ID FROM C_BPartner WHERE AD_Client_ID=? AND Value='TATUNG-STATIONERY'",
                clientId);

            if (bpartnerId <= 0) {
                bpartnerId = DB.getSQLValue(trxName,
                    "SELECT MIN(C_BPartner_ID) FROM C_BPartner WHERE AD_Client_ID=? AND IsVendor='Y'",
                    clientId);
            }

            if (bpartnerId <= 0) {
                log.warning("找不到供應商，跳過採購單");
                return false;
            }

            MBPartner bp = new MBPartner(ctx, bpartnerId, trxName);

            // 取得 BP 地址（C_Order.C_BPartner_Location_ID 是 NOT NULL）
            MBPartnerLocation[] locs = bp.getLocations(false);
            if (locs == null || locs.length == 0) {
                log.warning("供應商沒有地址，跳過採購單: " + bp.getName());
                return false;
            }
            int bpLocId = locs[0].getC_BPartner_Location_ID();

            // 取得採購單據類型（NOT NULL）
            int docTypeId = DB.getSQLValue(trxName,
                "SELECT C_DocType_ID FROM C_DocType WHERE AD_Client_ID=? AND DocBaseType='POO' AND IsSOTrx='N' AND IsActive='Y'",
                clientId);
            if (docTypeId <= 0) {
                log.warning("找不到採購單據類型，跳過採購單");
                return false;
            }

            // 取得價格表（NOT NULL）
            int priceListId = DB.getSQLValue(trxName,
                "SELECT M_PriceList_ID FROM M_PriceList WHERE AD_Client_ID=? AND IsSOPriceList='N' AND IsActive='Y'",
                clientId);
            if (priceListId <= 0) {
                log.warning("找不到採購價格表，跳過採購單");
                return false;
            }

            // 取得付款條件（NOT NULL）
            int paymentTermId = DB.getSQLValue(trxName,
                "SELECT C_PaymentTerm_ID FROM C_PaymentTerm WHERE AD_Client_ID=? AND IsActive='Y' ORDER BY IsDefault DESC",
                clientId);
            if (paymentTermId <= 0) {
                log.warning("找不到付款條件，跳過採購單");
                return false;
            }

            // 取得業務代表（SalesRep_ID 在 AD_Column 標記為必填）
            // 優先使用 BP 的 SalesRep，否則使用 context 中的 AD_User_ID
            int salesRepId = bp.getSalesRep_ID();
            if (salesRepId <= 0) {
                salesRepId = Env.getAD_User_ID(ctx);
            }
            if (salesRepId <= 0) {
                // 查詢該 Client 的任何有效使用者
                salesRepId = DB.getSQLValue(trxName,
                    "SELECT MIN(AD_User_ID) FROM AD_User WHERE AD_Client_ID=? AND IsActive='Y'",
                    clientId);
            }

            // 建立採購單
            MOrder po = new MOrder(ctx, 0, trxName);
            po.setAD_Org_ID(orgId);
            po.setIsSOTrx(false);
            po.setC_DocTypeTarget_ID(docTypeId);
            po.setBPartner(bp);
            po.setC_BPartner_Location_ID(bpLocId);  // 明確設定地址（NOT NULL）
            po.setM_Warehouse_ID(warehouseId);
            po.setM_PriceList_ID(priceListId);      // 明確設定價格表（NOT NULL）
            po.setC_PaymentTerm_ID(paymentTermId);  // 明確設定付款條件（NOT NULL）
            if (salesRepId > 0) {
                po.setSalesRep_ID(salesRepId);      // 明確設定業務代表（AD_Column 標記必填）
            }
            po.setDateOrdered(new Timestamp(System.currentTimeMillis()));
            po.setDatePromised(new Timestamp(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)); // +7天
            po.setDescription("示範採購單 - 辦公用品補貨");

            if (!po.save()) {
                log.warning("無法建立採購單");
                return false;
            }

            // 建立採購單明細
            addOrderLine(ctx, po, "PAPER-A4-500", new BigDecimal("50"), trxName);
            addOrderLine(ctx, po, "PEN-BLUE", new BigDecimal("100"), trxName);
            addOrderLine(ctx, po, "STAPLER", new BigDecimal("10"), trxName);

            log.info("已建立採購單: " + po.getDocumentNo());
            return true;

        } catch (Exception e) {
            log.warning("建立採購單時發生錯誤: " + e.getMessage());
            return false;
        }
    }

    /**
     * 建立銷售訂單
     */
    private static boolean createSalesOrder(Properties ctx, int clientId, int orgId,
            int warehouseId, String trxName) {

        try {
            // 取得客戶
            int bpartnerId = DB.getSQLValue(trxName,
                "SELECT C_BPartner_ID FROM C_BPartner WHERE AD_Client_ID=? AND Value='ESLITE'",
                clientId);

            if (bpartnerId <= 0) {
                bpartnerId = DB.getSQLValue(trxName,
                    "SELECT MIN(C_BPartner_ID) FROM C_BPartner WHERE AD_Client_ID=? AND IsCustomer='Y'",
                    clientId);
            }

            if (bpartnerId <= 0) {
                log.warning("找不到客戶，跳過銷售訂單");
                return false;
            }

            MBPartner bp = new MBPartner(ctx, bpartnerId, trxName);

            // 取得 BP 地址（C_Order.C_BPartner_Location_ID 是 NOT NULL）
            MBPartnerLocation[] locs = bp.getLocations(false);
            if (locs == null || locs.length == 0) {
                log.warning("客戶沒有地址，跳過銷售訂單: " + bp.getName());
                return false;
            }
            int bpLocId = locs[0].getC_BPartner_Location_ID();

            // 取得銷售訂單單據類型（NOT NULL）
            int docTypeId = DB.getSQLValue(trxName,
                "SELECT C_DocType_ID FROM C_DocType WHERE AD_Client_ID=? AND DocBaseType='SOO' AND IsSOTrx='Y' AND IsActive='Y'",
                clientId);
            if (docTypeId <= 0) {
                log.warning("找不到銷售訂單單據類型，跳過銷售訂單");
                return false;
            }

            // 取得價格表（NOT NULL）
            int priceListId = DB.getSQLValue(trxName,
                "SELECT M_PriceList_ID FROM M_PriceList WHERE AD_Client_ID=? AND IsSOPriceList='Y' AND IsActive='Y'",
                clientId);
            if (priceListId <= 0) {
                log.warning("找不到銷售價格表，跳過銷售訂單");
                return false;
            }

            // 取得付款條件（NOT NULL）
            int paymentTermId = DB.getSQLValue(trxName,
                "SELECT C_PaymentTerm_ID FROM C_PaymentTerm WHERE AD_Client_ID=? AND IsActive='Y' ORDER BY IsDefault DESC",
                clientId);
            if (paymentTermId <= 0) {
                log.warning("找不到付款條件，跳過銷售訂單");
                return false;
            }

            // 取得業務代表（SalesRep_ID 在 AD_Column 標記為必填）
            // 優先使用 BP 的 SalesRep，否則使用 context 中的 AD_User_ID
            int salesRepId = bp.getSalesRep_ID();
            if (salesRepId <= 0) {
                salesRepId = Env.getAD_User_ID(ctx);
            }
            if (salesRepId <= 0) {
                // 查詢該 Client 的任何有效使用者
                salesRepId = DB.getSQLValue(trxName,
                    "SELECT MIN(AD_User_ID) FROM AD_User WHERE AD_Client_ID=? AND IsActive='Y'",
                    clientId);
            }

            // 建立銷售訂單
            MOrder so = new MOrder(ctx, 0, trxName);
            so.setAD_Org_ID(orgId);
            so.setIsSOTrx(true);
            so.setC_DocTypeTarget_ID(docTypeId);
            so.setBPartner(bp);
            so.setC_BPartner_Location_ID(bpLocId);  // 明確設定地址（NOT NULL）
            so.setM_Warehouse_ID(warehouseId);
            so.setM_PriceList_ID(priceListId);      // 明確設定價格表（NOT NULL）
            so.setC_PaymentTerm_ID(paymentTermId);  // 明確設定付款條件（NOT NULL）
            if (salesRepId > 0) {
                so.setSalesRep_ID(salesRepId);      // 明確設定業務代表（AD_Column 標記必填）
            }
            so.setDateOrdered(new Timestamp(System.currentTimeMillis()));
            so.setDatePromised(new Timestamp(System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000)); // +3天
            so.setDescription("示範銷售訂單 - 文具採購");

            if (!so.save()) {
                log.warning("無法建立銷售訂單");
                return false;
            }

            // 建立銷售訂單明細
            addOrderLine(ctx, so, "PAPER-A4-500", new BigDecimal("10"), trxName);
            addOrderLine(ctx, so, "NOTEBOOK-25K", new BigDecimal("20"), trxName);
            addOrderLine(ctx, so, "PEN-BLUE", new BigDecimal("50"), trxName);
            addOrderLine(ctx, so, "FOLDER-A4", new BigDecimal("30"), trxName);

            log.info("已建立銷售訂單: " + so.getDocumentNo());
            return true;

        } catch (Exception e) {
            log.warning("建立銷售訂單時發生錯誤: " + e.getMessage());
            return false;
        }
    }

    /**
     * 新增訂單明細
     */
    private static boolean addOrderLine(Properties ctx, MOrder order, String productValue,
            BigDecimal qty, String trxName) {

        int productId = DB.getSQLValue(trxName,
            "SELECT M_Product_ID FROM M_Product WHERE AD_Client_ID=? AND Value=?",
            order.getAD_Client_ID(), productValue);

        if (productId <= 0) {
            log.fine("找不到商品: " + productValue);
            return false;
        }

        MProduct product = new MProduct(ctx, productId, trxName);

        MOrderLine line = new MOrderLine(order);
        line.setProduct(product);
        line.setQty(qty);

        return line.save();
    }
}
