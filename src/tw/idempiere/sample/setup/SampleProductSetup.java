/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MProductPrice;
import org.compiere.model.MUOM;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;

import tw.idempiere.sample.data.ProductData;

/**
 * 商品建立
 * <p>
 * 負責建立示範公司的商品資料，包含：
 * <ul>
 *   <li>商品類別（6 個）</li>
 *   <li>計量單位（動態從資料中提取）</li>
 *   <li>庫存品項（20 筆）</li>
 *   <li>服務項目（5 筆）</li>
 *   <li>組合商品和 BOM（5 筆）</li>
 *   <li>商品價格</li>
 * </ul>
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class SampleProductSetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleProductSetup.class);

    /** 商品類別對照表（Value -> MProductCategory） */
    private static Map<String, MProductCategory> categoryMap = new HashMap<>();

    /** UOM 對照表（Symbol -> C_UOM_ID） */
    private static Map<String, Integer> uomMap = new HashMap<>();

    /** 商品對照表（Value -> MProduct） */
    private static Map<String, MProduct> productMap = new HashMap<>();

    /** 稅務類別 ID */
    private static int taxCategoryId = 0;

    /** 銷售價格表版本 */
    private static MPriceListVersion salesPLV;

    /** 採購價格表版本 */
    private static MPriceListVersion purchasePLV;

    /** 私有建構子，防止實例化 */
    private SampleProductSetup() {
        // 工具類別，不需要實例化
    }

    /** Standard 價格表版本（MSetup 自動建立的） */
    private static MPriceListVersion standardPLV;

    /**
     * 建立所有商品
     * <p>
     * 建立流程：
     * <ol>
     *   <li>建立商品類別</li>
     *   <li>設定計量單位</li>
     *   <li>建立庫存品項</li>
     *   <li>建立服務項目</li>
     *   <li>建立組合商品</li>
     *   <li>建立 BOM 結構</li>
     * </ol>
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param taxCatId 稅務類別 ID
     * @param salesPriceListVersion 銷售價格表版本
     * @param purchasePriceListVersion 採購價格表版本
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    public static boolean createProducts(Properties ctx, int clientId, int taxCatId,
            MPriceListVersion salesPriceListVersion, MPriceListVersion purchasePriceListVersion,
            String trxName) {

        log.info("開始建立商品...");

        // 設定靜態變數
        categoryMap.clear();
        uomMap.clear();
        productMap.clear();
        taxCategoryId = taxCatId;
        salesPLV = salesPriceListVersion;
        purchasePLV = purchasePriceListVersion;

        // 取得 MSetup 自動建立的 Standard 價格表版本
        // 這是為了讓所有商品都有價格，避免 MInventoryLine 驗證失敗
        standardPLV = getStandardPriceListVersion(ctx, clientId, trxName);

        try {
            // 步驟 1：建立商品類別
            if (!createCategories(ctx, clientId, trxName)) {
                log.severe("無法建立商品類別");
                return false;
            }

            // 步驟 2：設定 UOM（從資料中動態提取）
            if (!setupUOMs(ctx, trxName)) {
                log.severe("無法設定 UOM");
                return false;
            }

            // 步驟 3：建立庫存品項（20 筆）
            // 格式：{商品代碼, 商品名稱, 類別代碼, 單位, 進價, 售價}
            for (Object[] data : ProductData.ITEMS) {
                MProduct product = createProduct(ctx, clientId,
                        (String) data[0],       // value
                        (String) data[1],       // name
                        (String) data[2],       // category
                        (String) data[3],       // uom
                        MProduct.PRODUCTTYPE_Item,
                        (BigDecimal) data[4],   // purchase price
                        (BigDecimal) data[5],   // sales price
                        trxName);
                if (product == null) {
                    log.warning("無法建立商品: " + data[1] + "，繼續處理其他資料");
                }
            }

            // 步驟 4：建立服務項目（5 筆）
            // 格式：{服務代碼, 服務名稱, 單位, 售價}
            for (Object[] data : ProductData.SERVICES) {
                MProduct product = createProduct(ctx, clientId,
                        (String) data[0],       // value
                        (String) data[1],       // name
                        "SERVICE",              // category（固定為 SERVICE）
                        (String) data[2],       // uom
                        MProduct.PRODUCTTYPE_Service,
                        null,                   // 服務無進價
                        (BigDecimal) data[3],   // sales price
                        trxName);
                if (product == null) {
                    log.warning("無法建立服務: " + data[1] + "，繼續處理其他資料");
                }
            }

            // 步驟 5：建立組合商品（5 筆）
            // 格式：{組合品代碼, 組合品名稱, 單位, 售價}
            for (Object[] data : ProductData.BOMS) {
                MProduct product = createProduct(ctx, clientId,
                        (String) data[0],       // value
                        (String) data[1],       // name
                        "GENERAL",              // category（組合商品使用一般商品類別）
                        (String) data[2],       // uom
                        MProduct.PRODUCTTYPE_Item,
                        null,                   // 組合商品無採購價
                        (BigDecimal) data[3],   // sales price
                        trxName);
                if (product == null) {
                    log.warning("無法建立組合商品: " + data[1] + "，繼續處理其他資料");
                    continue;
                }

                // 設定為 BOM 母件
                product.setIsBOM(true);
                if (!product.save()) {
                    log.warning("無法設定 BOM 標記: " + data[1]);
                }
            }

            // 步驟 6：建立 BOM 結構
            if (!createBOMs(ctx, clientId, trxName)) {
                log.warning("BOM 結構建立不完整");
            }

            log.info("商品建立完成，共 " + productMap.size() + " 項");
            return true;

        } catch (Exception e) {
            log.severe("建立商品時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 建立商品類別
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    private static boolean createCategories(Properties ctx, int clientId, String trxName) {
        log.info("建立商品類別...");

        // 格式：{類別代碼, 類別名稱, 類別說明}
        for (String[] data : ProductData.CATEGORIES) {
            String value = data[0];
            String name = data[1];
            String description = data[2];

            // 檢查是否已存在
            int existingId = DB.getSQLValue(trxName,
                "SELECT M_Product_Category_ID FROM M_Product_Category WHERE AD_Client_ID=? AND Value=?",
                clientId, value);

            if (existingId > 0) {
                MProductCategory existing = new MProductCategory(ctx, existingId, trxName);
                categoryMap.put(value, existing);
                log.fine("商品類別已存在，跳過: " + name);
                continue;
            }

            MProductCategory cat = new MProductCategory(ctx, 0, trxName);
            cat.setAD_Org_ID(0);
            cat.setValue(value);
            cat.setName(name);
            cat.setDescription(description);

            if (!cat.save()) {
                log.warning("無法儲存商品類別: " + name + "，跳過");
                continue;
            }

            categoryMap.put(value, cat);
            log.fine("已建立商品類別: " + name);
        }

        log.info("商品類別處理完成，共 " + categoryMap.size() + " 個");
        return true;
    }

    /**
     * 設定計量單位
     * <p>
     * 從 ITEMS、SERVICES、BOMS 資料中動態收集所有使用的單位，
     * 然後查詢或建立對應的 UOM。
     * </p>
     *
     * @param ctx 系統上下文
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    private static boolean setupUOMs(Properties ctx, String trxName) {
        log.info("設定計量單位...");

        // 收集所有使用的單位
        Set<String> uomSymbols = new HashSet<>();

        // 從 ITEMS 收集（索引 3 是單位）
        for (Object[] data : ProductData.ITEMS) {
            uomSymbols.add((String) data[3]);
        }

        // 從 SERVICES 收集（索引 2 是單位）
        for (Object[] data : ProductData.SERVICES) {
            uomSymbols.add((String) data[2]);
        }

        // 從 BOMS 收集（索引 2 是單位）
        for (Object[] data : ProductData.BOMS) {
            uomSymbols.add((String) data[2]);
        }

        // 單位中文名稱對照
        Map<String, String> uomNames = new HashMap<>();
        uomNames.put("包", "包");
        uomNames.put("箱", "箱");
        uomNames.put("本", "本");
        uomNames.put("支", "支");
        uomNames.put("個", "個");
        uomNames.put("盒", "盒");
        uomNames.put("台", "台");
        uomNames.put("把", "把");
        uomNames.put("捲", "捲");
        uomNames.put("次", "次");
        uomNames.put("組", "組");

        // 取得當前 Client ID
        int clientId = Env.getAD_Client_ID(ctx);

        // 設定每個 UOM
        for (String symbol : uomSymbols) {
            String name = uomNames.getOrDefault(symbol, symbol);

            // 先查詢系統級別的 UOM（AD_Client_ID = 0）或當前 Client 的 UOM
            String sql = "SELECT C_UOM_ID FROM C_UOM WHERE (AD_Client_ID = 0 OR AD_Client_ID = ?) AND (UOMSymbol = ? OR X12DE355 = ? OR Name = ?)";
            int uomId = DB.getSQLValue(trxName, sql, clientId, symbol, symbol, name);

            if (uomId <= 0) {
                // 建立新 UOM
                MUOM uom = new MUOM(ctx, 0, trxName);
                uom.setAD_Org_ID(0);
                uom.setUOMSymbol(symbol);
                uom.setX12DE355(symbol);
                uom.setName(name);

                if (!uom.save()) {
                    log.warning("無法儲存 UOM: " + name + "，使用系統預設 EA");
                    // 使用系統預設的 EA
                    uomId = DB.getSQLValue(trxName, "SELECT C_UOM_ID FROM C_UOM WHERE X12DE355 = 'EA'");
                    if (uomId <= 0) {
                        log.severe("找不到系統預設 UOM (EA)");
                        return false;
                    }
                } else {
                    uomId = uom.getC_UOM_ID();
                    log.fine("已建立 UOM: " + symbol + " - " + name);
                }
            } else {
                log.fine("使用現有 UOM: " + symbol);
            }

            uomMap.put(symbol, uomId);
        }

        log.info("UOM 設定完成，共 " + uomMap.size() + " 個");
        return true;
    }

    /**
     * 建立單一商品
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param value 代碼
     * @param name 名稱
     * @param categoryValue 商品類別代碼
     * @param uomSymbol UOM 符號
     * @param productType 商品類型
     * @param purchasePrice 採購價（可為 null）
     * @param salesPrice 銷售價（可為 null）
     * @param trxName 交易名稱
     * @return 建立的 MProduct，失敗時回傳 null
     */
    private static MProduct createProduct(Properties ctx, int clientId, String value,
            String name, String categoryValue, String uomSymbol, String productType,
            BigDecimal purchasePrice, BigDecimal salesPrice, String trxName) {

        // 檢查是否已存在
        int existingId = DB.getSQLValue(trxName,
            "SELECT M_Product_ID FROM M_Product WHERE AD_Client_ID=? AND Value=?",
            clientId, value);

        if (existingId > 0) {
            MProduct existing = new MProduct(ctx, existingId, trxName);
            productMap.put(value, existing);
            log.fine("商品已存在，跳過: " + name);
            return existing;
        }

        // 取得商品類別
        MProductCategory category = categoryMap.get(categoryValue);

        // 取得 UOM ID
        Integer uomId = uomMap.get(uomSymbol);
        if (uomId == null) {
            // 嘗試從系統找 EA
            uomId = DB.getSQLValue(trxName, "SELECT C_UOM_ID FROM C_UOM WHERE X12DE355 = 'EA'");
            if (uomId <= 0) {
                log.severe("找不到 UOM: " + uomSymbol + " 也找不到 EA");
                return null;
            }
        }

        MProduct product = new MProduct(ctx, 0, trxName);
        product.setAD_Org_ID(0);
        product.setValue(value);
        product.setName(name);
        product.setProductType(productType);
        product.setC_UOM_ID(uomId);

        // 設定商品類別
        if (category != null) {
            product.setM_Product_Category_ID(category.getM_Product_Category_ID());
        }

        // 設定稅務類別
        if (taxCategoryId > 0) {
            product.setC_TaxCategory_ID(taxCategoryId);
        }

        // 設定商品屬性
        product.setIsPurchased(true);
        product.setIsSold(true);
        product.setIsStocked(MProduct.PRODUCTTYPE_Item.equals(productType));

        if (!product.save()) {
            log.severe("無法儲存商品: " + name);
            return null;
        }

        // 建立價格
        if (salesPrice != null && salesPLV != null) {
            createProductPrice(product, salesPLV, salesPrice, trxName);
        }
        if (purchasePrice != null && purchasePLV != null) {
            createProductPrice(product, purchasePLV, purchasePrice, trxName);
        }

        // 也加到 Standard 價格表（避免 MInventoryLine 驗證失敗）
        if (standardPLV != null) {
            // 使用銷售價格或採購價格（優先使用銷售價格）
            java.math.BigDecimal stdPrice = salesPrice != null ? salesPrice : purchasePrice;
            if (stdPrice != null) {
                createProductPrice(product, standardPLV, stdPrice, trxName);
            }
        }

        productMap.put(value, product);
        log.fine("已建立商品: " + value + " - " + name);
        return product;
    }

    /**
     * 建立商品價格
     *
     * @param product 商品
     * @param plv 價格表版本
     * @param price 價格
     * @param trxName 交易名稱
     */
    private static void createProductPrice(MProduct product, MPriceListVersion plv,
            BigDecimal price, String trxName) {

        MProductPrice pp = new MProductPrice(plv.getCtx(), 0, trxName);
        pp.setM_PriceList_Version_ID(plv.getM_PriceList_Version_ID());
        pp.setM_Product_ID(product.getM_Product_ID());
        pp.setPriceList(price);
        pp.setPriceStd(price);
        pp.setPriceLimit(price);

        if (!pp.save()) {
            log.warning("無法儲存商品價格: " + product.getValue());
        }
    }

    /**
     * 建立 BOM 結構
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    private static boolean createBOMs(Properties ctx, int clientId, String trxName) {
        log.info("開始建立 BOM...");

        // 依據 BOM 母件分組
        Map<String, MPPProductBOM> bomMap = new HashMap<>();

        // 格式：{組合品代碼, 元件商品代碼, 數量}
        for (Object[] data : ProductData.BOM_LINES) {
            String bomValue = (String) data[0];
            String componentValue = (String) data[1];
            BigDecimal qty = (BigDecimal) data[2];

            // 取得 BOM 母件和元件商品
            MProduct bomProduct = productMap.get(bomValue);
            MProduct component = productMap.get(componentValue);

            if (bomProduct == null) {
                log.warning("找不到 BOM 母件: " + bomValue);
                continue;
            }
            if (component == null) {
                log.warning("找不到 BOM 元件: " + componentValue);
                continue;
            }

            // 取得或建立 BOM Header
            MPPProductBOM bom = bomMap.get(bomValue);
            if (bom == null) {
                bom = new MPPProductBOM(ctx, 0, trxName);
                bom.setAD_Org_ID(0);
                bom.setM_Product_ID(bomProduct.getM_Product_ID());
                bom.setValue(bomValue);
                bom.setName(bomProduct.getName() + " BOM");
                bom.setBOMType(MPPProductBOM.BOMTYPE_CurrentActive);
                bom.setBOMUse(MPPProductBOM.BOMUSE_Master);
                bom.setC_UOM_ID(bomProduct.getC_UOM_ID());

                if (!bom.save()) {
                    log.severe("無法儲存 BOM: " + bomValue);
                    continue;
                }

                bomMap.put(bomValue, bom);
            }

            // 建立 BOM Line
            MPPProductBOMLine line = new MPPProductBOMLine(bom);
            line.setM_Product_ID(component.getM_Product_ID());
            line.setQtyBOM(qty);
            line.setC_UOM_ID(component.getC_UOM_ID());

            if (!line.save()) {
                log.warning("無法儲存 BOM Line: " + bomValue + " -> " + componentValue);
            }
        }

        log.info("BOM 建立完成，共 " + bomMap.size() + " 組");
        return true;
    }

    /**
     * 取得商品對照表
     *
     * @return 商品代碼對應 MProduct 的 Map
     */
    public static Map<String, MProduct> getProductMap() {
        return new HashMap<>(productMap);
    }

    /**
     * 取得商品類別對照表
     *
     * @return 商品類別代碼對應 MProductCategory 的 Map
     */
    public static Map<String, MProductCategory> getCategoryMap() {
        return new HashMap<>(categoryMap);
    }

    /**
     * 取得特定商品
     *
     * @param value 商品代碼
     * @return MProduct 物件，找不到時回傳 null
     */
    public static MProduct getProduct(String value) {
        return productMap.get(value);
    }

    /**
     * 取得建立的商品數量
     *
     * @return 商品數量
     */
    public static int getProductCount() {
        return productMap.size();
    }

    /**
     * 取得 MSetup 自動建立的 Standard 價格表版本
     * <p>
     * MSetup 在建立 Client 時會自動建立名為 "Standard" 的價格表。
     * 為了讓所有商品都能通過 MInventoryLine 等模組的價格表驗證，
     * 需要將商品也加到這個價格表中。
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return Standard 價格表版本，找不到時回傳 null
     */
    private static MPriceListVersion getStandardPriceListVersion(Properties ctx, int clientId, String trxName) {
        // 查詢 Standard 價格表的版本
        String sql = "SELECT plv.M_PriceList_Version_ID " +
            "FROM M_PriceList_Version plv " +
            "JOIN M_PriceList pl ON plv.M_PriceList_ID = pl.M_PriceList_ID " +
            "WHERE pl.AD_Client_ID = ? AND pl.Name = 'Standard' " +
            "ORDER BY plv.ValidFrom DESC";

        int plvId = DB.getSQLValue(trxName, sql, clientId);

        if (plvId > 0) {
            log.info("找到 Standard 價格表版本: " + plvId);
            return new MPriceListVersion(ctx, plvId, trxName);
        }

        log.fine("未找到 Standard 價格表版本");
        return null;
    }
}
