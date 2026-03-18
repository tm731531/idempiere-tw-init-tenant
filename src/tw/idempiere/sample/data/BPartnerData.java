/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.data;

/**
 * 業務夥伴資料定義
 * <p>
 * 定義供應商、客戶、員工等業務夥伴資料。
 * 包含 5 個供應商、7 個客戶、3 個員工，共 15 筆。
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class BPartnerData {

    /**
     * 業務夥伴群組定義
     * <p>
     * 欄位順序：{群組代碼, 群組名稱, 是否預設}
     * </p>
     */
    public static final String[][] BP_GROUPS = {
        // {群組代碼, 群組名稱, 是否預設}
        {"VENDOR",   "供應商", "N"},
        {"CUSTOMER", "客戶",   "Y"},
        {"EMPLOYEE", "員工",   "N"}
    };

    /**
     * 供應商資料定義（5 筆）
     * <p>
     * 欄位順序：{代碼, 名稱, 說明}
     * </p>
     */
    public static final String[][] VENDORS = {
        // {代碼, 名稱, 說明}
        {"TATUNG-STATIONERY", "大同文具股份有限公司", "主要文具供應商"},
        {"KINGCAR-LOGISTICS", "金車物流有限公司",     "物流服務供應商"},
        {"VENDOR-A",          "供應商 A",             "一般供應商"},
        {"VENDOR-B",          "供應商 B",             "一般供應商"},
        {"VENDOR-C",          "供應商 C",             "一般供應商"}
    };

    /**
     * 客戶資料定義（7 筆）
     * <p>
     * 欄位順序：{代碼, 名稱, 說明}
     * </p>
     */
    public static final String[][] CUSTOMERS = {
        // {代碼, 名稱, 說明}
        {"ESLITE",      "誠品書店",       "大型零售客戶"},
        {"FAMILY-MART", "全家便利商店",   "連鎖通路客戶"},
        {"TAIPEI-GOV",  "台北市政府",     "政府機關客戶"},
        {"CUSTOMER-A",  "客戶 A",         "一般客戶"},
        {"CUSTOMER-B",  "客戶 B",         "一般客戶"},
        {"CUSTOMER-C",  "客戶 C",         "一般客戶"},
        {"CUSTOMER-D",  "客戶 D",         "一般客戶"}
    };

    /**
     * 員工資料定義（3 筆）
     * <p>
     * 欄位順序：{代碼, 名稱, 說明}
     * </p>
     */
    public static final String[][] EMPLOYEES = {
        // {代碼, 名稱, 說明}
        {"EMP-WANG", "王小明", "業務人員"},
        {"EMP-LEE",  "李小華", "倉管人員"},
        {"EMP-A",    "員工 A", "一般員工"}
    };

    /** 私有建構子，防止實例化 */
    private BPartnerData() {
        // 工具類別，不需要實例化
    }
}
