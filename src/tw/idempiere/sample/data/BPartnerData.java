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
 * 包含 5 個供應商、7 個客戶、3 個員工。
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class BPartnerData {

    /**
     * 業務夥伴群組定義
     * <p>
     * 欄位順序：{群組代碼, 群組名稱, 是否供應商, 是否客戶, 是否員工}
     * </p>
     */
    public static final String[][] BP_GROUPS = {
        // {群組代碼, 群組名稱, 是否供應商, 是否客戶, 是否員工}
        {"VENDOR",   "供應商",   "Y", "N", "N"},
        {"CUSTOMER", "客戶",     "N", "Y", "N"},
        {"EMPLOYEE", "員工",     "N", "N", "Y"}
    };

    /**
     * 供應商資料定義
     * <p>
     * 欄位順序：{供應商代碼, 供應商名稱, 統一編號, 聯絡人, 電話, 地址, 付款條件代碼}
     * </p>
     * <ul>
     *   <li>供應商代碼：供應商的唯一識別碼</li>
     *   <li>供應商名稱：供應商的公司名稱</li>
     *   <li>統一編號：台灣公司的 8 位數稅籍編號</li>
     *   <li>聯絡人：主要聯絡窗口姓名</li>
     *   <li>電話：聯絡電話</li>
     *   <li>地址：公司地址</li>
     *   <li>付款條件代碼：付款條件（如 N30 表示 30 天）</li>
     * </ul>
     */
    public static final String[][] VENDORS = {
        // {供應商代碼, 供應商名稱, 統一編號, 聯絡人, 電話, 地址, 付款條件代碼}
        {"V001", "台灣電子材料股份有限公司", "12345678", "王大明", "02-2345-6789", "台北市中山區南京東路100號", "N30"},
        {"V002", "宏達科技有限公司",         "23456789", "李小華", "02-8765-4321", "台北市內湖區瑞光路200號",   "N45"},
        {"V003", "中部五金行",               "34567890", "張志明", "04-2345-6789", "台中市西區民生路50號",       "N15"},
        {"V004", "南部塑膠工業股份有限公司", "45678901", "陳美玲", "07-3456-7890", "高雄市前鎮區中山路300號",   "N30"},
        {"V005", "東方物流股份有限公司",     "56789012", "林志偉", "03-4567-8901", "桃園市中壢區中正路400號",   "N60"}
    };

    /**
     * 客戶資料定義
     * <p>
     * 欄位順序：{客戶代碼, 客戶名稱, 統一編號, 聯絡人, 電話, 地址, 信用額度, 付款條件代碼}
     * </p>
     * <ul>
     *   <li>客戶代碼：客戶的唯一識別碼</li>
     *   <li>客戶名稱：客戶的公司名稱</li>
     *   <li>統一編號：台灣公司的 8 位數稅籍編號</li>
     *   <li>聯絡人：主要聯絡窗口姓名</li>
     *   <li>電話：聯絡電話</li>
     *   <li>地址：公司地址</li>
     *   <li>信用額度：授信金額（新台幣）</li>
     *   <li>付款條件代碼：付款條件</li>
     * </ul>
     */
    public static final String[][] CUSTOMERS = {
        // {客戶代碼, 客戶名稱, 統一編號, 聯絡人, 電話, 地址, 信用額度, 付款條件代碼}
        {"C001", "北方實業股份有限公司", "11111111", "周志豪", "02-1111-2222", "台北市信義區忠孝東路500號", "500000",  "N30"},
        {"C002", "新竹科技有限公司",     "22222222", "吳佳蓉", "03-2222-3333", "新竹市東區光復路100號",     "1000000", "N30"},
        {"C003", "中華資訊股份有限公司", "33333333", "鄭文哲", "02-3333-4444", "台北市大安區敦化南路200號", "800000",  "N45"},
        {"C004", "台中貿易商行",         "44444444", "黃淑芬", "04-4444-5555", "台中市北區三民路150號",     "300000",  "N15"},
        {"C005", "高雄國際企業有限公司", "55555555", "許志宏", "07-5555-6666", "高雄市苓雅區中正路250號",   "600000",  "N30"},
        {"C006", "東部農產運銷合作社",   "66666666", "蔡明德", "03-6666-7777", "花蓮市中山路100號",         "200000",  "N60"},
        {"C007", "零售通路股份有限公司", "77777777", "劉美君", "02-7777-8888", "台北市松山區民生東路350號", "1500000", "N30"}
    };

    /**
     * 員工資料定義
     * <p>
     * 欄位順序：{員工代碼, 員工姓名, 部門, 職稱, 電話, 電子郵件, 所屬組織代碼}
     * </p>
     * <ul>
     *   <li>員工代碼：員工的唯一識別碼</li>
     *   <li>員工姓名：員工的全名</li>
     *   <li>部門：所屬部門名稱</li>
     *   <li>職稱：工作職稱</li>
     *   <li>電話：聯絡電話（分機）</li>
     *   <li>電子郵件：公司電子郵件地址</li>
     *   <li>所屬組織代碼：員工服務的組織</li>
     * </ul>
     */
    public static final String[][] EMPLOYEES = {
        // {員工代碼, 員工姓名, 部門, 職稱, 電話, 電子郵件, 所屬組織代碼}
        {"E001", "陳志明", "業務部", "業務經理",   "ext.101", "chen.zm@sample.com.tw",  "HQ"},
        {"E002", "林佳慧", "財務部", "會計專員",   "ext.201", "lin.jh@sample.com.tw",   "HQ"},
        {"E003", "王建國", "倉儲部", "倉管人員",   "ext.301", "wang.jg@sample.com.tw",  "TC"}
    };

    // 群組欄位索引常數
    /** 群組代碼欄位索引 */
    public static final int GRP_VALUE = 0;
    /** 群組名稱欄位索引 */
    public static final int GRP_NAME = 1;
    /** 是否供應商欄位索引 */
    public static final int GRP_IS_VENDOR = 2;
    /** 是否客戶欄位索引 */
    public static final int GRP_IS_CUSTOMER = 3;
    /** 是否員工欄位索引 */
    public static final int GRP_IS_EMPLOYEE = 4;

    // 供應商欄位索引常數
    /** 供應商代碼欄位索引 */
    public static final int VND_VALUE = 0;
    /** 供應商名稱欄位索引 */
    public static final int VND_NAME = 1;
    /** 統一編號欄位索引 */
    public static final int VND_TAXID = 2;
    /** 聯絡人欄位索引 */
    public static final int VND_CONTACT = 3;
    /** 電話欄位索引 */
    public static final int VND_PHONE = 4;
    /** 地址欄位索引 */
    public static final int VND_ADDRESS = 5;
    /** 付款條件欄位索引 */
    public static final int VND_PAYTERM = 6;

    // 客戶欄位索引常數
    /** 客戶代碼欄位索引 */
    public static final int CUS_VALUE = 0;
    /** 客戶名稱欄位索引 */
    public static final int CUS_NAME = 1;
    /** 統一編號欄位索引 */
    public static final int CUS_TAXID = 2;
    /** 聯絡人欄位索引 */
    public static final int CUS_CONTACT = 3;
    /** 電話欄位索引 */
    public static final int CUS_PHONE = 4;
    /** 地址欄位索引 */
    public static final int CUS_ADDRESS = 5;
    /** 信用額度欄位索引 */
    public static final int CUS_CREDIT = 6;
    /** 付款條件欄位索引 */
    public static final int CUS_PAYTERM = 7;

    // 員工欄位索引常數
    /** 員工代碼欄位索引 */
    public static final int EMP_VALUE = 0;
    /** 員工姓名欄位索引 */
    public static final int EMP_NAME = 1;
    /** 部門欄位索引 */
    public static final int EMP_DEPT = 2;
    /** 職稱欄位索引 */
    public static final int EMP_TITLE = 3;
    /** 電話欄位索引 */
    public static final int EMP_PHONE = 4;
    /** 電子郵件欄位索引 */
    public static final int EMP_EMAIL = 5;
    /** 所屬組織欄位索引 */
    public static final int EMP_ORG = 6;

    /** 私有建構子，防止實例化 */
    private BPartnerData() {
        // 工具類別，不需要實例化
    }
}
