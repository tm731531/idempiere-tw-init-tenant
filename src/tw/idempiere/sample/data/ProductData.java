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
 * 定義商品類別、計量單位、庫存品、服務及 BOM 組合品。
 * 包含 6 個類別、7 個計量單位、20 個庫存品、5 個服務、5 個組合品。
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class ProductData {

    /**
     * 商品類別定義
     * <p>
     * 欄位順序：{類別代碼, 類別名稱, 類別說明, 是否自行生產}
     * </p>
     * <ul>
     *   <li>類別代碼：類別的唯一識別碼</li>
     *   <li>類別名稱：類別的顯示名稱</li>
     *   <li>類別說明：類別的詳細描述</li>
     *   <li>是否自行生產：Y=自行生產，N=外購</li>
     * </ul>
     */
    public static final String[][] CATEGORIES = {
        // {類別代碼, 類別名稱, 類別說明, 是否自行生產}
        {"ELEC",    "電子零件",   "電子元件、IC、被動元件等",       "N"},
        {"MECH",    "機械零件",   "螺絲、螺帽、軸承等機械零件",     "N"},
        {"PACK",    "包裝材料",   "紙箱、膠帶、氣泡袋等包材",       "N"},
        {"OFFICE",  "辦公用品",   "文具、紙張、辦公設備等",         "N"},
        {"ASSEM",   "組裝成品",   "自行組裝的成品",                 "Y"},
        {"SERVICE", "服務",       "維修、安裝、諮詢等服務項目",     "N"}
    };

    /**
     * 計量單位定義
     * <p>
     * 欄位順序：{單位代碼, 單位名稱, 單位符號, 小數位數}
     * </p>
     * <ul>
     *   <li>單位代碼：計量單位的唯一識別碼</li>
     *   <li>單位名稱：計量單位的中文名稱</li>
     *   <li>單位符號：計量單位的縮寫符號</li>
     *   <li>小數位數：允許的小數位數</li>
     * </ul>
     */
    public static final String[][] UOMS = {
        // {單位代碼, 單位名稱, 單位符號, 小數位數}
        {"EA",  "個",     "個",  "0"},
        {"PCS", "件",     "件",  "0"},
        {"SET", "組",     "組",  "0"},
        {"BOX", "箱",     "箱",  "0"},
        {"KG",  "公斤",   "kg",  "2"},
        {"M",   "公尺",   "m",   "2"},
        {"HR",  "小時",   "hr",  "1"}
    };

    /**
     * 庫存品定義
     * <p>
     * 欄位順序：{商品代碼, 商品名稱, 類別代碼, 單位代碼, 進價, 售價}
     * </p>
     * <ul>
     *   <li>商品代碼：商品的唯一識別碼</li>
     *   <li>商品名稱：商品的顯示名稱</li>
     *   <li>類別代碼：商品所屬的類別</li>
     *   <li>單位代碼：商品的計量單位</li>
     *   <li>進價：採購單價（新台幣）</li>
     *   <li>售價：銷售單價（新台幣）</li>
     * </ul>
     */
    public static final Object[][] ITEMS = {
        // {商品代碼, 商品名稱, 類別代碼, 單位代碼, 進價, 售價}
        // 電子零件
        {"ELEC-001", "電阻 10K ohm",        "ELEC", "PCS", new BigDecimal("0.50"),   new BigDecimal("1.00")},
        {"ELEC-002", "電容 100uF",          "ELEC", "PCS", new BigDecimal("2.00"),   new BigDecimal("5.00")},
        {"ELEC-003", "LED 紅光 5mm",        "ELEC", "PCS", new BigDecimal("1.50"),   new BigDecimal("3.50")},
        {"ELEC-004", "LED 綠光 5mm",        "ELEC", "PCS", new BigDecimal("1.50"),   new BigDecimal("3.50")},
        {"ELEC-005", "IC 晶片 ATmega328",   "ELEC", "PCS", new BigDecimal("85.00"),  new BigDecimal("150.00")},
        {"ELEC-006", "USB 連接器 Type-C",   "ELEC", "PCS", new BigDecimal("15.00"),  new BigDecimal("35.00")},
        {"ELEC-007", "電源供應器 5V 2A",    "ELEC", "EA",  new BigDecimal("120.00"), new BigDecimal("250.00")},

        // 機械零件
        {"MECH-001", "螺絲 M3x10mm",        "MECH", "PCS", new BigDecimal("0.30"),   new BigDecimal("0.80")},
        {"MECH-002", "螺帽 M3",             "MECH", "PCS", new BigDecimal("0.20"),   new BigDecimal("0.50")},
        {"MECH-003", "墊片 M3",             "MECH", "PCS", new BigDecimal("0.10"),   new BigDecimal("0.30")},
        {"MECH-004", "軸承 608ZZ",          "MECH", "EA",  new BigDecimal("25.00"),  new BigDecimal("55.00")},
        {"MECH-005", "鋁擠型 2020 (1M)",    "MECH", "M",   new BigDecimal("80.00"),  new BigDecimal("150.00")},

        // 包裝材料
        {"PACK-001", "紙箱 小",             "PACK", "EA",  new BigDecimal("8.00"),   new BigDecimal("15.00")},
        {"PACK-002", "紙箱 中",             "PACK", "EA",  new BigDecimal("15.00"),  new BigDecimal("28.00")},
        {"PACK-003", "紙箱 大",             "PACK", "EA",  new BigDecimal("25.00"),  new BigDecimal("45.00")},
        {"PACK-004", "氣泡袋 A4",           "PACK", "PCS", new BigDecimal("3.00"),   new BigDecimal("8.00")},
        {"PACK-005", "膠帶 透明 48mm",      "PACK", "EA",  new BigDecimal("25.00"),  new BigDecimal("45.00")},

        // 辦公用品
        {"OFFC-001", "A4 影印紙 (500張)",   "OFFICE", "BOX", new BigDecimal("95.00"),  new BigDecimal("150.00")},
        {"OFFC-002", "原子筆 藍色",         "OFFICE", "PCS", new BigDecimal("5.00"),   new BigDecimal("12.00")},
        {"OFFC-003", "釘書機",              "OFFICE", "EA",  new BigDecimal("35.00"),  new BigDecimal("80.00")}
    };

    /**
     * 服務項目定義
     * <p>
     * 欄位順序：{服務代碼, 服務名稱, 類別代碼, 單位代碼, 服務費用}
     * </p>
     * <ul>
     *   <li>服務代碼：服務的唯一識別碼</li>
     *   <li>服務名稱：服務的顯示名稱</li>
     *   <li>類別代碼：服務所屬的類別</li>
     *   <li>單位代碼：服務的計量單位</li>
     *   <li>服務費用：服務的單價（新台幣）</li>
     * </ul>
     */
    public static final Object[][] SERVICES = {
        // {服務代碼, 服務名稱, 類別代碼, 單位代碼, 服務費用}
        {"SVC-001", "產品安裝服務",     "SERVICE", "HR", new BigDecimal("800.00")},
        {"SVC-002", "設備維修服務",     "SERVICE", "HR", new BigDecimal("1000.00")},
        {"SVC-003", "技術諮詢服務",     "SERVICE", "HR", new BigDecimal("1500.00")},
        {"SVC-004", "教育訓練服務",     "SERVICE", "HR", new BigDecimal("2000.00")},
        {"SVC-005", "現場支援服務",     "SERVICE", "HR", new BigDecimal("1200.00")}
    };

    /**
     * BOM 組合品定義
     * <p>
     * 欄位順序：{組合品代碼, 組合品名稱, 類別代碼, 單位代碼, 售價}
     * </p>
     * <ul>
     *   <li>組合品代碼：組合品的唯一識別碼</li>
     *   <li>組合品名稱：組合品的顯示名稱</li>
     *   <li>類別代碼：組合品所屬的類別</li>
     *   <li>單位代碼：組合品的計量單位</li>
     *   <li>售價：組合品的銷售單價（新台幣）</li>
     * </ul>
     */
    public static final Object[][] BOMS = {
        // {組合品代碼, 組合品名稱, 類別代碼, 單位代碼, 售價}
        {"BOM-001", "LED 指示燈模組",         "ASSEM", "SET", new BigDecimal("180.00")},
        {"BOM-002", "Arduino 開發板套件",     "ASSEM", "SET", new BigDecimal("650.00")},
        {"BOM-003", "機械手臂基座組",         "ASSEM", "SET", new BigDecimal("1200.00")},
        {"BOM-004", "USB 電源模組",           "ASSEM", "SET", new BigDecimal("450.00")},
        {"BOM-005", "包裝禮盒組 (中型)",      "ASSEM", "SET", new BigDecimal("120.00")}
    };

    /**
     * BOM 組成明細定義
     * <p>
     * 欄位順序：{組合品代碼, 元件商品代碼, 數量}
     * </p>
     * <ul>
     *   <li>組合品代碼：BOM 的父項目代碼</li>
     *   <li>元件商品代碼：BOM 的子項目代碼</li>
     *   <li>數量：該元件在 BOM 中的用量</li>
     * </ul>
     */
    public static final Object[][] BOM_LINES = {
        // {組合品代碼, 元件商品代碼, 數量}
        // BOM-001: LED 指示燈模組
        {"BOM-001", "ELEC-001", new BigDecimal("5")},     // 電阻 10K ohm x 5
        {"BOM-001", "ELEC-003", new BigDecimal("2")},     // LED 紅光 x 2
        {"BOM-001", "ELEC-004", new BigDecimal("2")},     // LED 綠光 x 2
        {"BOM-001", "MECH-001", new BigDecimal("4")},     // 螺絲 M3x10mm x 4
        {"BOM-001", "MECH-002", new BigDecimal("4")},     // 螺帽 M3 x 4

        // BOM-002: Arduino 開發板套件
        {"BOM-002", "ELEC-005", new BigDecimal("1")},     // IC 晶片 ATmega328 x 1
        {"BOM-002", "ELEC-002", new BigDecimal("10")},    // 電容 100uF x 10
        {"BOM-002", "ELEC-001", new BigDecimal("20")},    // 電阻 10K ohm x 20
        {"BOM-002", "ELEC-006", new BigDecimal("1")},     // USB 連接器 Type-C x 1
        {"BOM-002", "MECH-001", new BigDecimal("6")},     // 螺絲 M3x10mm x 6
        {"BOM-002", "MECH-002", new BigDecimal("6")},     // 螺帽 M3 x 6

        // BOM-003: 機械手臂基座組
        {"BOM-003", "MECH-004", new BigDecimal("4")},     // 軸承 608ZZ x 4
        {"BOM-003", "MECH-005", new BigDecimal("2")},     // 鋁擠型 2020 x 2M
        {"BOM-003", "MECH-001", new BigDecimal("20")},    // 螺絲 M3x10mm x 20
        {"BOM-003", "MECH-002", new BigDecimal("20")},    // 螺帽 M3 x 20
        {"BOM-003", "MECH-003", new BigDecimal("20")},    // 墊片 M3 x 20

        // BOM-004: USB 電源模組
        {"BOM-004", "ELEC-006", new BigDecimal("2")},     // USB 連接器 Type-C x 2
        {"BOM-004", "ELEC-007", new BigDecimal("1")},     // 電源供應器 5V 2A x 1
        {"BOM-004", "ELEC-002", new BigDecimal("5")},     // 電容 100uF x 5
        {"BOM-004", "MECH-001", new BigDecimal("4")},     // 螺絲 M3x10mm x 4
        {"BOM-004", "MECH-002", new BigDecimal("4")},     // 螺帽 M3 x 4

        // BOM-005: 包裝禮盒組 (中型)
        {"BOM-005", "PACK-002", new BigDecimal("1")},     // 紙箱 中 x 1
        {"BOM-005", "PACK-004", new BigDecimal("3")},     // 氣泡袋 A4 x 3
        {"BOM-005", "PACK-005", new BigDecimal("1")}      // 膠帶 透明 48mm x 1
    };

    // 類別欄位索引常數
    /** 類別代碼欄位索引 */
    public static final int CAT_VALUE = 0;
    /** 類別名稱欄位索引 */
    public static final int CAT_NAME = 1;
    /** 類別說明欄位索引 */
    public static final int CAT_DESC = 2;
    /** 是否自行生產欄位索引 */
    public static final int CAT_IS_MANUFACTURED = 3;

    // 計量單位欄位索引常數
    /** 單位代碼欄位索引 */
    public static final int UOM_VALUE = 0;
    /** 單位名稱欄位索引 */
    public static final int UOM_NAME = 1;
    /** 單位符號欄位索引 */
    public static final int UOM_SYMBOL = 2;
    /** 小數位數欄位索引 */
    public static final int UOM_PRECISION = 3;

    // 庫存品/組合品欄位索引常數
    /** 商品代碼欄位索引 */
    public static final int ITEM_VALUE = 0;
    /** 商品名稱欄位索引 */
    public static final int ITEM_NAME = 1;
    /** 類別代碼欄位索引 */
    public static final int ITEM_CATEGORY = 2;
    /** 單位代碼欄位索引 */
    public static final int ITEM_UOM = 3;
    /** 進價欄位索引（庫存品使用） */
    public static final int ITEM_COST = 4;
    /** 售價欄位索引 */
    public static final int ITEM_PRICE = 5;

    // 服務欄位索引常數
    /** 服務代碼欄位索引 */
    public static final int SVC_VALUE = 0;
    /** 服務名稱欄位索引 */
    public static final int SVC_NAME = 1;
    /** 類別代碼欄位索引 */
    public static final int SVC_CATEGORY = 2;
    /** 單位代碼欄位索引 */
    public static final int SVC_UOM = 3;
    /** 服務費用欄位索引 */
    public static final int SVC_PRICE = 4;

    // BOM 明細欄位索引常數
    /** 組合品代碼欄位索引 */
    public static final int BOM_PARENT = 0;
    /** 元件商品代碼欄位索引 */
    public static final int BOM_COMPONENT = 1;
    /** 數量欄位索引 */
    public static final int BOM_QTY = 2;

    /** 私有建構子，防止實例化 */
    private ProductData() {
        // 工具類別，不需要實例化
    }
}
