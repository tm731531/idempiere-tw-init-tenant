/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.data;

/**
 * 台灣會計科目定義
 * <p>
 * 依據台灣商業會計法規定義會計科目架構，
 * 採用五位數科目代碼，第一碼表示科目類型：
 * </p>
 * <ul>
 *   <li>1xxx - 資產類科目</li>
 *   <li>2xxx - 負債類科目</li>
 *   <li>3xxx - 權益類科目</li>
 *   <li>4xxx - 收入類科目</li>
 *   <li>5xxx - 費用類科目</li>
 * </ul>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class ChartOfAccountsTW {

    // ========== 科目類型常數 ==========
    /** 資產類科目 (Asset) */
    public static final String TYPE_ASSET = "A";
    /** 負債類科目 (Liability) */
    public static final String TYPE_LIABILITY = "L";
    /** 權益類科目 (Owner's Equity) */
    public static final String TYPE_EQUITY = "O";
    /** 收入類科目 (Revenue) */
    public static final String TYPE_REVENUE = "R";
    /** 費用類科目 (Expense) */
    public static final String TYPE_EXPENSE = "E";

    /**
     * 會計科目定義
     * <p>
     * 欄位順序：{科目代碼, 科目名稱, 科目類型, 是否彙總, 父科目代碼}
     * </p>
     * <ul>
     *   <li>科目代碼：會計科目的唯一識別碼</li>
     *   <li>科目名稱：科目的中文名稱</li>
     *   <li>科目類型：A=資產, L=負債, O=權益, R=收入, E=費用</li>
     *   <li>是否彙總：Y=彙總科目（不能過帳），N=明細科目</li>
     *   <li>父科目代碼：上層科目的代碼，空值表示最上層</li>
     * </ul>
     */
    public static final String[][] ACCOUNTS = {
        // ==================== 1xxx 資產類 ====================
        // {科目代碼, 科目名稱, 科目類型, 是否彙總, 父科目代碼}
        {"1000", "資產",           TYPE_ASSET, "Y", ""},

        // 流動資產
        {"1100", "流動資產",       TYPE_ASSET, "Y", "1000"},
        {"1101", "現金",           TYPE_ASSET, "N", "1100"},
        {"1102", "零用金",         TYPE_ASSET, "N", "1100"},
        {"1103", "銀行存款",       TYPE_ASSET, "N", "1100"},
        {"1104", "銀行存款-台幣", TYPE_ASSET, "N", "1103"},
        {"1105", "銀行存款-美金", TYPE_ASSET, "N", "1103"},
        {"1110", "應收帳款",       TYPE_ASSET, "N", "1100"},
        {"1111", "應收票據",       TYPE_ASSET, "N", "1100"},
        {"1112", "其他應收款",     TYPE_ASSET, "N", "1100"},
        {"1120", "預付款項",       TYPE_ASSET, "N", "1100"},
        {"1121", "預付貨款",       TYPE_ASSET, "N", "1120"},
        {"1122", "預付費用",       TYPE_ASSET, "N", "1120"},

        // 存貨
        {"1130", "存貨",           TYPE_ASSET, "Y", "1100"},
        {"1131", "商品存貨",       TYPE_ASSET, "N", "1130"},
        {"1132", "原物料",         TYPE_ASSET, "N", "1130"},
        {"1133", "在製品",         TYPE_ASSET, "N", "1130"},
        {"1134", "製成品",         TYPE_ASSET, "N", "1130"},

        // 固定資產
        {"1200", "固定資產",       TYPE_ASSET, "Y", "1000"},
        {"1201", "土地",           TYPE_ASSET, "N", "1200"},
        {"1202", "房屋及建築",     TYPE_ASSET, "N", "1200"},
        {"1203", "累計折舊-房屋", TYPE_ASSET, "N", "1202"},
        {"1204", "機器設備",       TYPE_ASSET, "N", "1200"},
        {"1205", "累計折舊-機器", TYPE_ASSET, "N", "1204"},
        {"1206", "運輸設備",       TYPE_ASSET, "N", "1200"},
        {"1207", "累計折舊-運輸", TYPE_ASSET, "N", "1206"},
        {"1208", "辦公設備",       TYPE_ASSET, "N", "1200"},
        {"1209", "累計折舊-辦公", TYPE_ASSET, "N", "1208"},

        // ==================== 2xxx 負債類 ====================
        {"2000", "負債",           TYPE_LIABILITY, "Y", ""},

        // 流動負債
        {"2100", "流動負債",       TYPE_LIABILITY, "Y", "2000"},
        {"2101", "應付帳款",       TYPE_LIABILITY, "N", "2100"},
        {"2102", "應付票據",       TYPE_LIABILITY, "N", "2100"},
        {"2103", "其他應付款",     TYPE_LIABILITY, "N", "2100"},
        {"2110", "應付薪資",       TYPE_LIABILITY, "N", "2100"},
        {"2111", "應付獎金",       TYPE_LIABILITY, "N", "2100"},
        {"2120", "應付稅捐",       TYPE_LIABILITY, "N", "2100"},
        {"2121", "應付營業稅",     TYPE_LIABILITY, "N", "2120"},
        {"2122", "應付所得稅",     TYPE_LIABILITY, "N", "2120"},
        {"2130", "預收款項",       TYPE_LIABILITY, "N", "2100"},
        {"2131", "預收貨款",       TYPE_LIABILITY, "N", "2130"},

        // 長期負債
        {"2200", "長期負債",       TYPE_LIABILITY, "Y", "2000"},
        {"2201", "長期借款",       TYPE_LIABILITY, "N", "2200"},
        {"2202", "長期應付款",     TYPE_LIABILITY, "N", "2200"},

        // ==================== 3xxx 權益類 ====================
        {"3000", "權益",           TYPE_EQUITY, "Y", ""},
        {"3100", "股本",           TYPE_EQUITY, "Y", "3000"},
        {"3101", "普通股股本",     TYPE_EQUITY, "N", "3100"},
        {"3200", "資本公積",       TYPE_EQUITY, "Y", "3000"},
        {"3201", "股票溢價",       TYPE_EQUITY, "N", "3200"},
        {"3300", "保留盈餘",       TYPE_EQUITY, "Y", "3000"},
        {"3301", "法定盈餘公積",   TYPE_EQUITY, "N", "3300"},
        {"3302", "未分配盈餘",     TYPE_EQUITY, "N", "3300"},
        {"3303", "本期損益",       TYPE_EQUITY, "N", "3300"},

        // ==================== 4xxx 收入類 ====================
        {"4000", "收入",           TYPE_REVENUE, "Y", ""},
        {"4100", "營業收入",       TYPE_REVENUE, "Y", "4000"},
        {"4101", "銷貨收入",       TYPE_REVENUE, "N", "4100"},
        {"4102", "銷貨退回",       TYPE_REVENUE, "N", "4100"},
        {"4103", "銷貨折讓",       TYPE_REVENUE, "N", "4100"},
        {"4104", "服務收入",       TYPE_REVENUE, "N", "4100"},
        {"4200", "營業外收入",     TYPE_REVENUE, "Y", "4000"},
        {"4201", "利息收入",       TYPE_REVENUE, "N", "4200"},
        {"4202", "租金收入",       TYPE_REVENUE, "N", "4200"},
        {"4203", "處分資產利益",   TYPE_REVENUE, "N", "4200"},
        {"4204", "兌換利益",       TYPE_REVENUE, "N", "4200"},
        {"4205", "其他收入",       TYPE_REVENUE, "N", "4200"},

        // ==================== 5xxx 費用類 ====================
        {"5000", "費用",           TYPE_EXPENSE, "Y", ""},
        {"5100", "營業成本",       TYPE_EXPENSE, "Y", "5000"},
        {"5101", "銷貨成本",       TYPE_EXPENSE, "N", "5100"},
        {"5102", "進貨",           TYPE_EXPENSE, "N", "5100"},
        {"5103", "進貨退出",       TYPE_EXPENSE, "N", "5100"},
        {"5104", "進貨折讓",       TYPE_EXPENSE, "N", "5100"},
        {"5105", "製造費用",       TYPE_EXPENSE, "N", "5100"},

        {"5200", "營業費用",       TYPE_EXPENSE, "Y", "5000"},
        {"5210", "薪資費用",       TYPE_EXPENSE, "N", "5200"},
        {"5211", "伙食費",         TYPE_EXPENSE, "N", "5200"},
        {"5212", "加班費",         TYPE_EXPENSE, "N", "5200"},
        {"5213", "勞健保費",       TYPE_EXPENSE, "N", "5200"},
        {"5214", "退休金費用",     TYPE_EXPENSE, "N", "5200"},
        {"5220", "租金費用",       TYPE_EXPENSE, "N", "5200"},
        {"5221", "水電費",         TYPE_EXPENSE, "N", "5200"},
        {"5222", "電話費",         TYPE_EXPENSE, "N", "5200"},
        {"5223", "郵電費",         TYPE_EXPENSE, "N", "5200"},
        {"5230", "文具用品",       TYPE_EXPENSE, "N", "5200"},
        {"5231", "旅費",           TYPE_EXPENSE, "N", "5200"},
        {"5232", "交際費",         TYPE_EXPENSE, "N", "5200"},
        {"5233", "廣告費",         TYPE_EXPENSE, "N", "5200"},
        {"5234", "運費",           TYPE_EXPENSE, "N", "5200"},
        {"5235", "保險費",         TYPE_EXPENSE, "N", "5200"},
        {"5236", "修繕費",         TYPE_EXPENSE, "N", "5200"},
        {"5237", "折舊費用",       TYPE_EXPENSE, "N", "5200"},
        {"5238", "稅捐",           TYPE_EXPENSE, "N", "5200"},
        {"5239", "呆帳費用",       TYPE_EXPENSE, "N", "5200"},
        {"5240", "雜費",           TYPE_EXPENSE, "N", "5200"},

        {"5300", "營業外費用",     TYPE_EXPENSE, "Y", "5000"},
        {"5301", "利息費用",       TYPE_EXPENSE, "N", "5300"},
        {"5302", "處分資產損失",   TYPE_EXPENSE, "N", "5300"},
        {"5303", "兌換損失",       TYPE_EXPENSE, "N", "5300"},
        {"5304", "其他損失",       TYPE_EXPENSE, "N", "5300"},

        {"5400", "所得稅費用",     TYPE_EXPENSE, "N", "5000"}
    };

    /**
     * 預設科目對應
     * <p>
     * 定義 iDempiere 會計模組需要的預設科目對應關係。
     * 欄位順序：{對應類型, 科目代碼}
     * </p>
     * <ul>
     *   <li>對應類型：iDempiere 預設科目的識別碼</li>
     *   <li>科目代碼：對應的會計科目代碼</li>
     * </ul>
     */
    public static final String[][] DEFAULT_ACCOUNTS = {
        // {對應類型, 科目代碼}
        // 銀行相關
        {"B_ASSET",                 "1103"},  // 銀行資產
        {"B_INTRANSIT",             "1103"},  // 在途資金
        {"B_INTERESTREV",           "4201"},  // 利息收入
        {"B_INTERESTEXP",           "5301"},  // 利息費用
        {"B_PAYMENTSELECT",         "2101"},  // 付款選擇
        {"B_UNALLOCATEDCASH",       "1103"},  // 未分配現金

        // 應收帳款相關
        {"C_RECEIVABLE",            "1110"},  // 應收帳款
        {"C_PREPAYMENT",            "1121"},  // 預付款

        // 應付帳款相關
        {"V_LIABILITY",             "2101"},  // 應付帳款
        {"V_PREPAYMENT",            "2131"},  // 預收款

        // 存貨相關
        {"P_ASSET",                 "1131"},  // 商品資產
        {"P_COGS",                  "5101"},  // 銷貨成本
        {"P_REVENUE",               "4101"},  // 銷貨收入
        {"P_PURCHASEPRICEVARIANCE", "5102"},  // 採購價差
        {"P_INVOICEPRICEVARIANCE",  "5102"},  // 發票價差
        {"P_TRADEDISCOUNTREC",      "4103"},  // 銷貨折讓
        {"P_TRADEDISCOUNTGRANT",    "5104"},  // 進貨折讓

        // 薪資相關
        {"E_EXPENSE",               "5210"},  // 薪資費用
        {"E_PREPAYMENT",            "1122"},  // 預付費用

        // 稅務相關
        {"T_DUE",                   "2121"},  // 應付稅額
        {"T_CREDIT",                "1112"},  // 進項稅額
        {"T_EXPENSE",               "5238"},  // 稅捐費用

        // 其他
        {"SUSPENSEBALANCING",       "3303"},  // 暫記平衡
        {"SUSPENSEERROR",           "3303"},  // 暫記錯誤
        {"CURRENCYBALANCING",       "4204"},  // 匯兌平衡
        {"RETAINEDEARNING",         "3302"},  // 保留盈餘
        {"INCOMESUMMARY",           "3303"},  // 本期損益
        {"INTERCOMPANYDUETO",       "2103"},  // 公司間應付
        {"INTERCOMPANYDUEFROM",     "1112"}   // 公司間應收
    };

    // 科目欄位索引常數
    /** 科目代碼欄位索引 */
    public static final int ACCT_VALUE = 0;
    /** 科目名稱欄位索引 */
    public static final int ACCT_NAME = 1;
    /** 科目類型欄位索引 */
    public static final int ACCT_TYPE = 2;
    /** 是否彙總科目欄位索引 */
    public static final int ACCT_IS_SUMMARY = 3;
    /** 父科目代碼欄位索引 */
    public static final int ACCT_PARENT = 4;

    // 預設科目欄位索引常數
    /** 對應類型欄位索引 */
    public static final int DEF_TYPE = 0;
    /** 科目代碼欄位索引 */
    public static final int DEF_ACCOUNT = 1;

    /** 私有建構子，防止實例化 */
    private ChartOfAccountsTW() {
        // 工具類別，不需要實例化
    }
}
