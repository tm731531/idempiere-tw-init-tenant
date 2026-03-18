/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.math.BigDecimal;
import java.util.Properties;

import org.compiere.model.MTax;
import org.compiere.model.MTaxCategory;
import org.compiere.util.CLogger;

/**
 * 稅務設定建立
 * <p>
 * 負責建立示範公司的稅務設定，包含：
 * <ul>
 *   <li>稅務類別（營業稅）</li>
 *   <li>營業稅 5%（預設稅率）</li>
 *   <li>免稅（0%）</li>
 * </ul>
 * </p>
 * <p>
 * 台灣營業稅說明：
 * <ul>
 *   <li>標準稅率：5%（加值型營業稅）</li>
 *   <li>適用範圍：銷售貨物或勞務</li>
 *   <li>免稅情況：出口貨物、特定貨物或勞務</li>
 * </ul>
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class SampleTaxSetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleTaxSetup.class);

    /** 稅務類別名稱 */
    private static final String TAX_CATEGORY_NAME = "台灣營業稅";

    /** 營業稅名稱 */
    private static final String TAX_NAME_5 = "營業稅 5%";

    /** 免稅名稱 */
    private static final String TAX_NAME_EXEMPT = "免稅";

    /** 營業稅率 5% */
    private static final BigDecimal TAX_RATE_5 = new BigDecimal("5");

    /** 免稅稅率 0% */
    private static final BigDecimal TAX_RATE_0 = BigDecimal.ZERO;

    /** 建立的稅務類別 ID */
    private static int taxCategoryId = 0;

    /** 建立的營業稅 5% ID */
    private static int taxId5 = 0;

    /** 建立的免稅 ID */
    private static int taxIdExempt = 0;

    /** 私有建構子，防止實例化 */
    private SampleTaxSetup() {
        // 工具類別，不需要實例化
    }

    /**
     * 建立稅務設定
     * <p>
     * 建立流程：
     * <ol>
     *   <li>建立稅務類別（MTaxCategory）</li>
     *   <li>建立營業稅 5%（MTax）- 設為預設</li>
     *   <li>建立免稅 0%（MTax）</li>
     * </ol>
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param orgId 組織 ID（通常為 0，表示 Client 層級）
     * @param trxName 交易名稱
     * @return 建立的 MTaxCategory，失敗時回傳 null
     */
    public static MTaxCategory createTax(Properties ctx, int clientId, int orgId, String trxName) {
        log.info("開始建立稅務設定...");

        // 清空 ID
        taxCategoryId = 0;
        taxId5 = 0;
        taxIdExempt = 0;

        try {
            // 步驟 1：建立稅務類別
            MTaxCategory taxCategory = createTaxCategory(ctx, clientId, trxName);
            if (taxCategory == null) {
                log.severe("無法建立稅務類別");
                return null;
            }
            taxCategoryId = taxCategory.getC_TaxCategory_ID();

            // 步驟 2：建立營業稅 5%（設為預設）
            MTax tax5 = createTaxRate(ctx, clientId, taxCategory, TAX_NAME_5, TAX_RATE_5, true, trxName);
            if (tax5 == null) {
                log.severe("無法建立營業稅 5%");
                return null;
            }
            taxId5 = tax5.getC_Tax_ID();

            // 步驟 3：建立免稅
            MTax taxExempt = createTaxRate(ctx, clientId, taxCategory, TAX_NAME_EXEMPT, TAX_RATE_0, false, trxName);
            if (taxExempt == null) {
                log.severe("無法建立免稅");
                return null;
            }
            taxIdExempt = taxExempt.getC_Tax_ID();

            log.info("稅務設定建立完成");
            return taxCategory;

        } catch (Exception e) {
            log.severe("建立稅務設定時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 建立稅務類別
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return 建立的 MTaxCategory，失敗時回傳 null
     */
    private static MTaxCategory createTaxCategory(Properties ctx, int clientId, String trxName) {
        log.info("建立稅務類別: " + TAX_CATEGORY_NAME);

        MTaxCategory taxCategory = new MTaxCategory(ctx, 0, trxName);
        taxCategory.setAD_Client_ID(clientId);
        taxCategory.setAD_Org_ID(0); // 稅務類別設定在 Client 層級
        taxCategory.setName(TAX_CATEGORY_NAME);
        taxCategory.setDescription("台灣加值型營業稅類別");
        taxCategory.setIsDefault(true); // 設為預設稅務類別

        if (!taxCategory.save()) {
            log.severe("無法儲存稅務類別");
            return null;
        }

        log.info("已建立稅務類別: " + TAX_CATEGORY_NAME + " (ID=" + taxCategory.getC_TaxCategory_ID() + ")");
        return taxCategory;
    }

    /**
     * 建立稅率
     * <p>
     * 稅率設定說明：
     * <ul>
     *   <li>SOPOType: Both - 銷售和採購都適用</li>
     *   <li>IsDocumentLevel: true - 在單據層級計算稅額</li>
     *   <li>IsSalesTax: false - 非美國銷售稅</li>
     *   <li>IsSummary: false - 非彙總稅率</li>
     * </ul>
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param taxCategory 稅務類別
     * @param name 稅率名稱
     * @param rate 稅率
     * @param isDefault 是否為預設稅率
     * @param trxName 交易名稱
     * @return 建立的 MTax，失敗時回傳 null
     */
    private static MTax createTaxRate(Properties ctx, int clientId, MTaxCategory taxCategory,
                                       String name, BigDecimal rate, boolean isDefault, String trxName) {
        log.info("建立稅率: " + name + " (" + rate + "%)");

        MTax tax = new MTax(ctx, 0, trxName);
        tax.setAD_Client_ID(clientId);
        tax.setAD_Org_ID(0); // 稅率設定在 Client 層級
        tax.setName(name);
        tax.setDescription(name + " - 台灣營業稅");
        tax.setC_TaxCategory_ID(taxCategory.getC_TaxCategory_ID());
        tax.setRate(rate);
        tax.setIsDefault(isDefault);

        // 稅務設定
        tax.setSOPOType(MTax.SOPOTYPE_Both); // 銷售和採購都適用
        tax.setIsDocumentLevel(true); // 單據層級計算稅額
        tax.setIsSalesTax(false); // 非美國銷售稅（台灣使用加值稅）
        tax.setIsSummary(false); // 非彙總稅率
        tax.setIsTaxExempt(rate.compareTo(BigDecimal.ZERO) == 0); // 稅率為 0 則為免稅
        tax.setRequiresTaxCertificate(false); // 不需要稅務證明

        // 有效期間（設定為長期有效）
        // ValidFrom 會使用預設值（當前日期）

        if (!tax.save()) {
            log.severe("無法儲存稅率: " + name);
            return null;
        }

        log.info("已建立稅率: " + name + " (ID=" + tax.getC_Tax_ID() + ")");
        return tax;
    }

    /**
     * 取得稅務類別 ID
     *
     * @return 稅務類別 ID，未建立時回傳 0
     */
    public static int getTaxCategoryId() {
        return taxCategoryId;
    }

    /**
     * 取得營業稅 5% ID
     *
     * @return 營業稅 5% ID，未建立時回傳 0
     */
    public static int getTaxId5() {
        return taxId5;
    }

    /**
     * 取得免稅 ID
     *
     * @return 免稅 ID，未建立時回傳 0
     */
    public static int getTaxIdExempt() {
        return taxIdExempt;
    }
}
