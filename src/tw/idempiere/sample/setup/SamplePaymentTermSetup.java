/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MPaymentTerm;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import tw.idempiere.sample.util.SetupLog;

/**
 * 付款條件設定
 * <p>
 * 建立台灣常用的付款條件：
 * <ul>
 *   <li>現金 - 立即付款</li>
 *   <li>月結30天 - 常見 B2B 條件</li>
 *   <li>月結60天 - 大型客戶常用</li>
 * </ul>
 * </p>
 *
 * @author 天地人實業
 * @version 2.1.0
 */
public class SamplePaymentTermSetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SamplePaymentTermSetup.class);

    /** 付款條件對照表（Value -> MPaymentTerm） */
    private static Map<String, MPaymentTerm> paymentTermMap = new HashMap<>();

    /**
     * 付款條件資料定義
     * <p>
     * 欄位順序：{代碼, 名稱, 天數, 折扣百分比, 折扣天數}
     * </p>
     */
    private static final String[][] PAYMENT_TERMS = {
        // {代碼, 名稱, 天數, 折扣%, 折扣天數}
        {"Cash",  "現金",      "0",  "0", "0"},
        {"Net30", "月結30天",  "30", "0", "0"},
        {"Net60", "月結60天",  "60", "0", "0"}
    };

    /** 私有建構子，防止實例化 */
    private SamplePaymentTermSetup() {
        // 工具類別，不需要實例化
    }

    /**
     * 建立付款條件
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    public static boolean createPaymentTerms(Properties ctx, int clientId, String trxName) {
        log.info("開始建立付款條件...");
        SetupLog.log("付款條件", "開始建立付款條件");

        // 清空對照表
        paymentTermMap.clear();

        try {
            int created = 0;
            int skipped = 0;

            for (String[] data : PAYMENT_TERMS) {
                String value = data[0];
                String name = data[1];
                int netDays = Integer.parseInt(data[2]);
                int discountPercent = Integer.parseInt(data[3]);
                int discountDays = Integer.parseInt(data[4]);

                // 檢查是否已存在
                int existingId = DB.getSQLValue(trxName,
                    "SELECT C_PaymentTerm_ID FROM C_PaymentTerm WHERE AD_Client_ID=? AND Value=?",
                    clientId, value);

                if (existingId > 0) {
                    MPaymentTerm existing = new MPaymentTerm(ctx, existingId, trxName);
                    paymentTermMap.put(value, existing);
                    log.fine("付款條件已存在，跳過: " + name);
                    skipped++;
                    continue;
                }

                // 建立新的付款條件
                MPaymentTerm pt = new MPaymentTerm(ctx, 0, trxName);
                pt.setAD_Org_ID(0);
                pt.setValue(value);
                pt.setName(name);
                pt.setNetDays(netDays);
                pt.setGraceDays(0);  // 寬限天數（NOT NULL）
                pt.setDiscount(new java.math.BigDecimal(discountPercent));
                pt.setDiscountDays(discountDays);
                pt.setDiscount2(java.math.BigDecimal.ZERO);  // 第二階段折扣（NOT NULL）
                pt.setDiscountDays2(0);  // 第二階段折扣天數（NOT NULL）
                pt.setIsValid(true);

                // 現金設為預設
                if ("Cash".equals(value)) {
                    pt.setIsDefault(true);
                }

                if (!pt.save()) {
                    log.warning("無法儲存付款條件: " + name);
                    continue;
                }

                paymentTermMap.put(value, pt);
                log.fine("已建立付款條件: " + value + " - " + name);
                created++;
            }

            SetupLog.log("付款條件", "完成，新建 " + created + " 個，跳過 " + skipped + " 個");
            log.info("付款條件建立完成，新建 " + created + " 個，跳過 " + skipped + " 個");
            return true;

        } catch (Exception e) {
            log.severe("建立付款條件時發生錯誤: " + e.getMessage());
            SetupLog.logError("付款條件", "建立失敗", e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 取得付款條件對照表
     *
     * @return 付款條件對照表（Value -> MPaymentTerm）
     */
    public static Map<String, MPaymentTerm> getPaymentTermMap() {
        return new HashMap<>(paymentTermMap);
    }

    /**
     * 取得指定的付款條件
     *
     * @param value 付款條件代碼
     * @return MPaymentTerm 物件，不存在時回傳 null
     */
    public static MPaymentTerm getPaymentTerm(String value) {
        return paymentTermMap.get(value);
    }

    /**
     * 取得現金付款條件 ID
     *
     * @return 現金付款條件 ID，不存在時回傳 0
     */
    public static int getCashPaymentTermId() {
        MPaymentTerm pt = paymentTermMap.get("Cash");
        return pt != null ? pt.getC_PaymentTerm_ID() : 0;
    }

    /**
     * 取得月結30天付款條件 ID
     *
     * @return 月結30天付款條件 ID，不存在時回傳 0
     */
    public static int getNet30PaymentTermId() {
        MPaymentTerm pt = paymentTermMap.get("Net30");
        return pt != null ? pt.getC_PaymentTerm_ID() : 0;
    }
}
