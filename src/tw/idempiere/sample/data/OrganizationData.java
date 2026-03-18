/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.data;

/**
 * 組織與倉庫資料定義
 * <p>
 * 定義示範公司的組織架構和倉庫配置。
 * 包含 3 個組織（總公司及分支機構）和 3 個倉庫。
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class OrganizationData {

    /**
     * 組織定義
     * <p>
     * 欄位順序：{組織代碼, 組織名稱, 組織說明, 是否彙總組織, 父組織代碼}
     * </p>
     * <ul>
     *   <li>組織代碼：組織的唯一識別碼</li>
     *   <li>組織名稱：組織的顯示名稱</li>
     *   <li>組織說明：組織的詳細描述</li>
     *   <li>是否彙總組織：Y=彙總組織（不能輸入交易），N=營運組織</li>
     *   <li>父組織代碼：上層組織的代碼，空值表示最上層</li>
     * </ul>
     */
    public static final String[][] ORGANIZATIONS = {
        // {組織代碼, 組織名稱, 組織說明, 是否彙總, 父組織代碼}
        {"HQ",      "台北總公司", "天地人實業有限公司總部，位於台北市",     "N", ""},
        {"TC",      "台中分公司", "中部區域營運中心，負責中部地區業務",     "N", "HQ"},
        {"KS",      "高雄倉庫",   "南部區域物流中心，負責南部地區配送",     "N", "HQ"}
    };

    /**
     * 倉庫定義
     * <p>
     * 欄位順序：{倉庫代碼, 倉庫名稱, 倉庫說明, 所屬組織代碼, 地址}
     * </p>
     * <ul>
     *   <li>倉庫代碼：倉庫的唯一識別碼</li>
     *   <li>倉庫名稱：倉庫的顯示名稱</li>
     *   <li>倉庫說明：倉庫的詳細描述</li>
     *   <li>所屬組織代碼：此倉庫歸屬的組織</li>
     *   <li>地址：倉庫的實體地址</li>
     * </ul>
     */
    public static final String[][] WAREHOUSES = {
        // {倉庫代碼, 倉庫名稱, 倉庫說明, 所屬組織代碼, 地址}
        {"WH-TP", "台北主倉", "總公司主要倉庫，存放高價值商品",     "HQ", "台北市內湖區瑞光路100號"},
        {"WH-TC", "台中倉",   "中部區域倉庫，供應中部經銷商",       "TC", "台中市西屯區工業區一路50號"},
        {"WH-KS", "高雄倉",   "南部區域倉庫，負責南部地區配送",     "KS", "高雄市前鎮區成功二路88號"}
    };

    /**
     * 儲位定義（每個倉庫的預設儲位）
     * <p>
     * 欄位順序：{儲位代碼, 儲位名稱, 所屬倉庫代碼, 是否預設儲位}
     * </p>
     */
    public static final String[][] LOCATORS = {
        // {儲位代碼, 儲位名稱, 所屬倉庫代碼, 是否預設}
        {"TP-A01", "台北A區01",   "WH-TP", "Y"},
        {"TP-A02", "台北A區02",   "WH-TP", "N"},
        {"TP-B01", "台北B區01",   "WH-TP", "N"},
        {"TC-A01", "台中A區01",   "WH-TC", "Y"},
        {"TC-A02", "台中A區02",   "WH-TC", "N"},
        {"KS-A01", "高雄A區01",   "WH-KS", "Y"},
        {"KS-A02", "高雄A區02",   "WH-KS", "N"}
    };

    // 組織欄位索引常數
    /** 組織代碼欄位索引 */
    public static final int ORG_VALUE = 0;
    /** 組織名稱欄位索引 */
    public static final int ORG_NAME = 1;
    /** 組織說明欄位索引 */
    public static final int ORG_DESC = 2;
    /** 是否彙總組織欄位索引 */
    public static final int ORG_IS_SUMMARY = 3;
    /** 父組織代碼欄位索引 */
    public static final int ORG_PARENT = 4;

    // 倉庫欄位索引常數
    /** 倉庫代碼欄位索引 */
    public static final int WH_VALUE = 0;
    /** 倉庫名稱欄位索引 */
    public static final int WH_NAME = 1;
    /** 倉庫說明欄位索引 */
    public static final int WH_DESC = 2;
    /** 所屬組織代碼欄位索引 */
    public static final int WH_ORG = 3;
    /** 倉庫地址欄位索引 */
    public static final int WH_ADDRESS = 4;

    // 儲位欄位索引常數
    /** 儲位代碼欄位索引 */
    public static final int LOC_VALUE = 0;
    /** 儲位名稱欄位索引 */
    public static final int LOC_NAME = 1;
    /** 所屬倉庫代碼欄位索引 */
    public static final int LOC_WAREHOUSE = 2;
    /** 是否預設儲位欄位索引 */
    public static final int LOC_IS_DEFAULT = 3;

    /** 私有建構子，防止實例化 */
    private OrganizationData() {
        // 工具類別，不需要實例化
    }
}
