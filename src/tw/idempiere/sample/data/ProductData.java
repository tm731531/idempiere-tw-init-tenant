/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.data;

import java.math.BigDecimal;

/**
 * 商品資料定義
 * <p>
 * 定義商品類別、庫存品、服務及 BOM 組合品。
 * 包含 6 個類別、20 個庫存品、5 個服務、5 個組合品，共 30 筆商品。
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class ProductData {

    /**
     * 商品類別定義（6 個）
     * <p>
     * 欄位順序：{類別代碼, 類別名稱, 類別說明}
     * </p>
     */
    public static final String[][] CATEGORIES = {
        // {類別代碼, 類別名稱, 類別說明}
        {"PAPER",      "紙類",     "影印紙、筆記本等"},
        {"WRITING",    "書寫工具", "原子筆、鉛筆等"},
        {"OFFICE",     "辦公用品", "釘書機、剪刀等"},
        {"FILE",       "檔案用品", "資料夾等"},
        {"SERVICE",    "服務",     "運費、安裝等"},
        {"GENERAL",    "一般商品", "其他商品"}
    };

    /**
     * 庫存品定義（20 筆）
     * <p>
     * 欄位順序：{商品代碼, 商品名稱, 類別代碼, 單位, 進價, 售價}
     * </p>
     */
    public static final Object[][] ITEMS = {
        // {商品代碼, 商品名稱, 類別代碼, 單位, 進價, 售價}
        // 紙類（4 筆）
        {"PAPER-A4-500",  "A4 影印紙（500張）", "PAPER",   "包", new BigDecimal("80"),   new BigDecimal("120")},
        {"PAPER-A4-BOX",  "A4 影印紙（箱裝）",  "PAPER",   "箱", new BigDecimal("750"),  new BigDecimal("1100")},
        {"NOTEBOOK-25K",  "筆記本-25K",         "PAPER",   "本", new BigDecimal("25"),   new BigDecimal("45")},
        {"NOTEBOOK-18K",  "筆記本-18K",         "PAPER",   "本", new BigDecimal("35"),   new BigDecimal("60")},

        // 書寫工具（5 筆）
        {"PEN-BLUE",      "原子筆-藍",          "WRITING", "支", new BigDecimal("8"),    new BigDecimal("15")},
        {"PEN-BLACK",     "原子筆-黑",          "WRITING", "支", new BigDecimal("8"),    new BigDecimal("15")},
        {"PEN-RED",       "原子筆-紅",          "WRITING", "支", new BigDecimal("8"),    new BigDecimal("15")},
        {"PENCIL-HB",     "鉛筆 HB",            "WRITING", "支", new BigDecimal("5"),    new BigDecimal("10")},
        {"ERASER",        "橡皮擦",             "WRITING", "個", new BigDecimal("8"),    new BigDecimal("15")},

        // 辦公用品（6 筆）
        {"CLIP-BOX",      "迴紋針（盒裝）",     "OFFICE",  "盒", new BigDecimal("15"),   new BigDecimal("25")},
        {"STAPLER",       "釘書機",             "OFFICE",  "台", new BigDecimal("60"),   new BigDecimal("120")},
        {"STAPLE-BOX",    "釘書針（盒裝）",     "OFFICE",  "盒", new BigDecimal("20"),   new BigDecimal("35")},
        {"SCISSORS",      "剪刀",               "OFFICE",  "把", new BigDecimal("40"),   new BigDecimal("80")},
        {"TAPE-DISPENSER","膠帶台",             "OFFICE",  "台", new BigDecimal("50"),   new BigDecimal("100")},
        {"TAPE-CLEAR",    "透明膠帶",           "OFFICE",  "捲", new BigDecimal("15"),   new BigDecimal("30")},

        // 檔案用品（1 筆）
        {"FOLDER-A4",     "資料夾-A4",          "FILE",    "個", new BigDecimal("12"),   new BigDecimal("25")},

        // 一般商品（4 筆）
        {"ITEM-A",        "商品 A",             "GENERAL", "個", new BigDecimal("50"),   new BigDecimal("100")},
        {"ITEM-B",        "商品 B",             "GENERAL", "個", new BigDecimal("80"),   new BigDecimal("150")},
        {"ITEM-C",        "商品 C",             "GENERAL", "個", new BigDecimal("30"),   new BigDecimal("60")},
        {"ITEM-D",        "商品 D",             "GENERAL", "個", new BigDecimal("45"),   new BigDecimal("90")}
    };

    /**
     * 服務項目定義（5 筆）
     * <p>
     * 欄位順序：{服務代碼, 服務名稱, 單位, 售價}
     * </p>
     */
    public static final Object[][] SERVICES = {
        // {服務代碼, 服務名稱, 單位, 售價}
        {"SVC-SHIPPING-TW", "國內運費",   "次", new BigDecimal("100")},
        {"SVC-INSTALL",     "安裝服務",   "次", new BigDecimal("500")},
        {"SVC-REPAIR",      "維修服務",   "次", new BigDecimal("800")},
        {"SVC-A",           "服務 A",     "次", new BigDecimal("300")},
        {"SVC-B",           "服務 B",     "次", new BigDecimal("400")}
    };

    /**
     * BOM 組合品定義（5 筆）
     * <p>
     * 欄位順序：{組合品代碼, 組合品名稱, 單位, 售價}
     * </p>
     */
    public static final Object[][] BOMS = {
        // {組合品代碼, 組合品名稱, 單位, 售價}
        {"BOM-GIFT-SET",    "文具禮盒組",   "組", new BigDecimal("150")},
        {"BOM-OFFICE-SET",  "辦公文具組",   "組", new BigDecimal("350")},
        {"BOM-STUDENT-SET", "學生文具組",   "組", new BigDecimal("120")},
        {"BOM-A",           "組合品 A",     "組", new BigDecimal("280")},
        {"BOM-B",           "組合品 B",     "組", new BigDecimal("320")}
    };

    /**
     * BOM 組成明細定義
     * <p>
     * 欄位順序：{組合品代碼, 元件商品代碼, 數量}
     * </p>
     */
    public static final Object[][] BOM_LINES = {
        // {組合品代碼, 元件商品代碼, 數量}

        // BOM-GIFT-SET: 文具禮盒組 = 原子筆-藍 x1 + 筆記本-25K x1 + 資料夾-A4 x1
        {"BOM-GIFT-SET", "PEN-BLUE",     new BigDecimal("1")},
        {"BOM-GIFT-SET", "NOTEBOOK-25K", new BigDecimal("1")},
        {"BOM-GIFT-SET", "FOLDER-A4",    new BigDecimal("1")},

        // BOM-OFFICE-SET: 辦公文具組 = 釘書機 x1 + 剪刀 x1 + 膠帶台 x1
        {"BOM-OFFICE-SET", "STAPLER",       new BigDecimal("1")},
        {"BOM-OFFICE-SET", "SCISSORS",      new BigDecimal("1")},
        {"BOM-OFFICE-SET", "TAPE-DISPENSER",new BigDecimal("1")},

        // BOM-STUDENT-SET: 學生文具組 = 鉛筆 HB x3 + 橡皮擦 x1 + 筆記本-25K x2
        {"BOM-STUDENT-SET", "PENCIL-HB",    new BigDecimal("3")},
        {"BOM-STUDENT-SET", "ERASER",       new BigDecimal("1")},
        {"BOM-STUDENT-SET", "NOTEBOOK-25K", new BigDecimal("2")},

        // BOM-A: 組合品 A = 商品 A x2 + 商品 B x1
        {"BOM-A", "ITEM-A", new BigDecimal("2")},
        {"BOM-A", "ITEM-B", new BigDecimal("1")},

        // BOM-B: 組合品 B = 商品 C x1 + 商品 D x3
        {"BOM-B", "ITEM-C", new BigDecimal("1")},
        {"BOM-B", "ITEM-D", new BigDecimal("3")}
    };

    /** 私有建構子，防止實例化 */
    private ProductData() {
        // 工具類別，不需要實例化
    }
}
