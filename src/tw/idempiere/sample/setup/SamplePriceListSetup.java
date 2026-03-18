/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.MPriceList;
import org.compiere.model.MPriceListVersion;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

/**
 * 價格表建立
 * <p>
 * 負責建立示範公司的價格表，包含：
 * <ul>
 *   <li>銷售價格表（標準售價）- 含稅價</li>
 *   <li>採購價格表（標準進價）- 含稅價</li>
 *   <li>對應的價格表版本</li>
 * </ul>
 * </p>
 * <p>
 * 使用台幣（TWD）作為幣別，若系統中無 TWD 則使用 USD 作為備用。
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class SamplePriceListSetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SamplePriceListSetup.class);

    /** 銷售價格表名稱 */
    private static final String SALES_PRICELIST_NAME = "標準售價";

    /** 採購價格表名稱 */
    private static final String PURCHASE_PRICELIST_NAME = "標準進價";

    /** 建立的銷售價格表 */
    private static MPriceList salesPriceList;

    /** 建立的採購價格表 */
    private static MPriceList purchasePriceList;

    /** 建立的銷售價格表版本 */
    private static MPriceListVersion salesPLV;

    /** 建立的採購價格表版本 */
    private static MPriceListVersion purchasePLV;

    /** 私有建構子，防止實例化 */
    private SamplePriceListSetup() {
        // 工具類別，不需要實例化
    }

    /**
     * 建立價格表
     * <p>
     * 建立流程：
     * <ol>
     *   <li>取得 TWD 幣別 ID</li>
     *   <li>建立銷售價格表和版本</li>
     *   <li>建立採購價格表和版本</li>
     * </ol>
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    public static boolean createPriceLists(Properties ctx, int clientId, String trxName) {
        log.info("開始建立價格表...");

        // 清空靜態變數
        salesPriceList = null;
        purchasePriceList = null;
        salesPLV = null;
        purchasePLV = null;

        try {
            int currencyId = getTWDCurrencyId(trxName);
            String currentYear = new java.text.SimpleDateFormat("yyyy").format(new java.util.Date());
            Timestamp validFrom = new Timestamp(System.currentTimeMillis());

            // 建立銷售價格表
            salesPriceList = createPriceList(ctx, clientId, SALES_PRICELIST_NAME,
                    "銷售用價格表", currencyId, true, trxName);
            if (salesPriceList == null) {
                log.severe("無法建立銷售價格表");
                return false;
            }

            // 建立銷售價格表版本
            salesPLV = createPriceListVersion(salesPriceList,
                    SALES_PRICELIST_NAME + " " + currentYear, validFrom, trxName);
            if (salesPLV == null) {
                log.severe("無法建立銷售價格表版本");
                return false;
            }

            // 建立採購價格表
            purchasePriceList = createPriceList(ctx, clientId, PURCHASE_PRICELIST_NAME,
                    "採購用價格表", currencyId, false, trxName);
            if (purchasePriceList == null) {
                log.severe("無法建立採購價格表");
                return false;
            }

            // 建立採購價格表版本
            purchasePLV = createPriceListVersion(purchasePriceList,
                    PURCHASE_PRICELIST_NAME + " " + currentYear, validFrom, trxName);
            if (purchasePLV == null) {
                log.severe("無法建立採購價格表版本");
                return false;
            }

            log.info("價格表建立完成");
            return true;

        } catch (Exception e) {
            log.severe("建立價格表時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 建立價格表
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param name 價格表名稱
     * @param description 說明
     * @param currencyId 幣別 ID
     * @param isSO 是否為銷售價格表
     * @param trxName 交易名稱
     * @return 建立的價格表，失敗時回傳 null
     */
    private static MPriceList createPriceList(Properties ctx, int clientId, String name,
            String description, int currencyId, boolean isSO, String trxName) {

        // 檢查是否已存在
        int existingId = DB.getSQLValue(trxName,
            "SELECT M_PriceList_ID FROM M_PriceList WHERE AD_Client_ID=? AND Name=?",
            clientId, name);

        if (existingId > 0) {
            log.info("價格表已存在，使用現有的: " + name);
            return new MPriceList(ctx, existingId, trxName);
        }

        MPriceList priceList = new MPriceList(ctx, 0, trxName);
        priceList.setAD_Org_ID(0);
        priceList.setName(name);
        priceList.setDescription(description);
        priceList.setC_Currency_ID(currencyId);
        priceList.setIsSOPriceList(isSO);
        priceList.setIsDefault(true);
        priceList.setIsTaxIncluded(true);

        if (!priceList.save()) {
            log.severe("無法儲存價格表: " + name);
            return null;
        }

        log.info("已建立價格表: " + name + " (ID=" + priceList.getM_PriceList_ID() + ")");
        return priceList;
    }

    /**
     * 建立價格表版本
     *
     * @param priceList 價格表
     * @param name 版本名稱
     * @param validFrom 生效日期
     * @param trxName 交易名稱
     * @return 建立的價格表版本，失敗時回傳 null
     */
    private static MPriceListVersion createPriceListVersion(MPriceList priceList,
            String name, Timestamp validFrom, String trxName) {

        // 檢查是否已存在
        int existingId = DB.getSQLValue(trxName,
            "SELECT M_PriceList_Version_ID FROM M_PriceList_Version WHERE M_PriceList_ID=?",
            priceList.getM_PriceList_ID());

        if (existingId > 0) {
            log.info("價格表版本已存在，使用現有的: " + name);
            return new MPriceListVersion(priceList.getCtx(), existingId, trxName);
        }

        MPriceListVersion plv = new MPriceListVersion(priceList);
        plv.setName(name);
        plv.setValidFrom(validFrom);

        // 設定折扣架構（必要欄位）
        int discountSchemaId = getOrCreateDiscountSchema(priceList.getCtx(),
                priceList.getAD_Client_ID(), trxName);
        if (discountSchemaId > 0) {
            plv.setM_DiscountSchema_ID(discountSchemaId);
        }

        if (!plv.save()) {
            log.severe("無法儲存價格表版本: " + name);
            return null;
        }

        log.info("已建立價格表版本: " + name + " (ID=" + plv.getM_PriceList_Version_ID() + ")");
        return plv;
    }

    /**
     * 取得或建立折扣架構
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return 折扣架構 ID
     */
    private static int getOrCreateDiscountSchema(java.util.Properties ctx, int clientId, String trxName) {
        // 先查詢是否已有折扣架構
        String sql = "SELECT M_DiscountSchema_ID FROM M_DiscountSchema WHERE AD_Client_ID=? AND IsActive='Y'";
        int schemaId = DB.getSQLValue(trxName, sql, clientId);

        if (schemaId > 0) {
            return schemaId;
        }

        // 建立預設折扣架構（無折扣）
        try {
            org.compiere.model.MDiscountSchema schema =
                new org.compiere.model.MDiscountSchema(ctx, 0, trxName);
            schema.setAD_Org_ID(0);
            schema.setName("標準價格");
            schema.setDiscountType(org.compiere.model.MDiscountSchema.DISCOUNTTYPE_Pricelist);
            schema.setFlatDiscount(java.math.BigDecimal.ZERO);

            if (schema.save()) {
                log.info("已建立折扣架構: 標準價格");
                return schema.getM_DiscountSchema_ID();
            }
        } catch (Exception e) {
            log.warning("無法建立折扣架構: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 取得 TWD 幣別 ID
     * <p>
     * 若系統中無 TWD 幣別，則使用 USD 作為備用。
     * </p>
     *
     * @param trxName 交易名稱
     * @return 幣別 ID
     */
    private static int getTWDCurrencyId(String trxName) {
        String sql = "SELECT C_Currency_ID FROM C_Currency WHERE ISO_Code = ?";
        int id = DB.getSQLValue(trxName, sql, "TWD");
        if (id <= 0) {
            id = DB.getSQLValue(trxName, sql, "USD");
            log.warning("找不到 TWD 幣別，使用 USD 作為備用");
        }
        return id > 0 ? id : 100; // 100 是 USD 的標準 ID
    }

    /**
     * 取得銷售價格表
     *
     * @return 銷售價格表，未建立時回傳 null
     */
    public static MPriceList getSalesPriceList() {
        return salesPriceList;
    }

    /**
     * 取得採購價格表
     *
     * @return 採購價格表，未建立時回傳 null
     */
    public static MPriceList getPurchasePriceList() {
        return purchasePriceList;
    }

    /**
     * 取得銷售價格表版本
     *
     * @return 銷售價格表版本，未建立時回傳 null
     */
    public static MPriceListVersion getSalesPLV() {
        return salesPLV;
    }

    /**
     * 取得採購價格表版本
     *
     * @return 採購價格表版本，未建立時回傳 null
     */
    public static MPriceListVersion getPurchasePLV() {
        return purchasePLV;
    }

    /**
     * 取得銷售價格表 ID
     *
     * @return 銷售價格表 ID，未建立時回傳 0
     */
    public static int getSalesPriceListId() {
        return salesPriceList != null ? salesPriceList.getM_PriceList_ID() : 0;
    }

    /**
     * 取得採購價格表 ID
     *
     * @return 採購價格表 ID，未建立時回傳 0
     */
    public static int getPurchasePriceListId() {
        return purchasePriceList != null ? purchasePriceList.getM_PriceList_ID() : 0;
    }
}
