/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MBPGroup;
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MLocation;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import tw.idempiere.sample.data.BPartnerData;
import tw.idempiere.sample.util.SetupLog;

/**
 * Business Partner 建立
 * <p>
 * 負責建立示範公司的業務夥伴資料，包含：
 * <ul>
 *   <li>BP 群組（供應商、客戶、員工）</li>
 *   <li>供應商（5 筆）- 含地址和統編</li>
 *   <li>客戶（7 筆）- 含地址和統編</li>
 *   <li>員工（3 筆）- 含地址</li>
 * </ul>
 * </p>
 *
 * @author 天地人實業
 * @version 2.1.0
 */
public class SampleBPSetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleBPSetup.class);

    /** BP 群組對照表（Value -> MBPGroup） */
    private static Map<String, MBPGroup> groupMap = new HashMap<>();

    /** BP 對照表（Value -> MBPartner） */
    private static Map<String, MBPartner> bpMap = new HashMap<>();

    /** 台灣國家 ID（快取） */
    private static int taiwanCountryId = 0;

    /** 私有建構子，防止實例化 */
    private SampleBPSetup() {
        // 工具類別，不需要實例化
    }

    /**
     * 建立所有 Business Partners
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    public static boolean createBPartners(Properties ctx, int clientId, String trxName) {
        log.info("開始建立 Business Partners...");
        SetupLog.log("業務夥伴", "開始建立業務夥伴（含地址和統編）");

        // 清空對照表
        groupMap.clear();
        bpMap.clear();

        // 取得台灣國家 ID
        taiwanCountryId = DB.getSQLValue(trxName,
            "SELECT C_Country_ID FROM C_Country WHERE CountryCode='TW'");
        if (taiwanCountryId <= 0) {
            taiwanCountryId = DB.getSQLValue(trxName,
                "SELECT C_Country_ID FROM C_Country WHERE CountryCode='US'");
        }

        try {
            // 步驟 1：建立 BP 群組
            if (!createBPGroups(ctx, clientId, trxName)) {
                log.severe("無法建立 BP 群組");
                return false;
            }

            int created = 0;

            // 步驟 2：建立供應商（5 筆）- 含地址和統編
            for (String[] data : BPartnerData.VENDORS) {
                MBPartner bp = createBPartnerWithLocation(ctx, clientId, data,
                        false, true, false, "VENDOR", trxName);
                if (bp != null) created++;
            }

            // 步驟 3：建立客戶（7 筆）- 含地址和統編
            for (String[] data : BPartnerData.CUSTOMERS) {
                MBPartner bp = createBPartnerWithLocation(ctx, clientId, data,
                        true, false, false, "CUSTOMER", trxName);
                if (bp != null) created++;
            }

            // 步驟 4：建立員工（3 筆）- 含地址
            for (String[] data : BPartnerData.EMPLOYEES) {
                MBPartner bp = createBPartnerWithLocation(ctx, clientId, data,
                        false, false, true, "EMPLOYEE", trxName);
                if (bp != null) created++;
            }

            SetupLog.log("業務夥伴", "完成，共建立 " + created + " 個業務夥伴（含地址）");
            log.info("Business Partners 建立完成，共 " + bpMap.size() + " 筆");
            return true;

        } catch (Exception e) {
            log.severe("建立 Business Partners 時發生錯誤: " + e.getMessage());
            SetupLog.logError("業務夥伴", "建立失敗", e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 建立 BP 群組
     */
    private static boolean createBPGroups(Properties ctx, int clientId, String trxName) {
        log.info("建立 BP 群組...");

        for (String[] data : BPartnerData.BP_GROUPS) {
            String value = data[0];
            String name = data[1];
            boolean isDefault = "Y".equals(data[2]);

            // 檢查是否已存在
            int existingId = DB.getSQLValue(trxName,
                "SELECT C_BP_Group_ID FROM C_BP_Group WHERE AD_Client_ID=? AND Value=?",
                clientId, value);

            if (existingId > 0) {
                MBPGroup existing = new MBPGroup(ctx, existingId, trxName);
                groupMap.put(value, existing);
                log.fine("BP 群組已存在，跳過: " + name);
                continue;
            }

            MBPGroup group = new MBPGroup(ctx, 0, trxName);
            group.setAD_Org_ID(0);
            group.setValue(value);
            group.setName(name);
            group.setIsDefault(isDefault);

            if (!group.save()) {
                log.warning("無法儲存 BP 群組: " + name + "，跳過");
                continue;
            }

            groupMap.put(value, group);
            log.fine("已建立 BP 群組: " + name);
        }

        log.info("BP 群組處理完成，共 " + groupMap.size() + " 個");
        return true;
    }

    /**
     * 建立單一 Business Partner（含地址和統編）
     * <p>
     * 資料陣列格式：{代碼, 名稱, 說明, 統編/身分證, 地址, 郵遞區號, 城市}
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param data 資料陣列
     * @param isCustomer 是否為客戶
     * @param isVendor 是否為供應商
     * @param isEmployee 是否為員工
     * @param groupValue BP 群組代碼
     * @param trxName 交易名稱
     * @return MBPartner 物件，失敗時回傳 null
     */
    private static MBPartner createBPartnerWithLocation(Properties ctx, int clientId,
            String[] data, boolean isCustomer, boolean isVendor, boolean isEmployee,
            String groupValue, String trxName) {

        String value = data[0];
        String name = data[1];
        String description = data[2];
        String taxId = data.length > 3 ? data[3] : null;
        String address = data.length > 4 ? data[4] : null;
        String postal = data.length > 5 ? data[5] : null;
        String city = data.length > 6 ? data[6] : null;

        // 檢查是否已存在
        int existingId = DB.getSQLValue(trxName,
            "SELECT C_BPartner_ID FROM C_BPartner WHERE AD_Client_ID=? AND Value=?",
            clientId, value);

        MBPartner bp;
        boolean isNew = false;

        if (existingId > 0) {
            bp = new MBPartner(ctx, existingId, trxName);
            bpMap.put(value, bp);
            log.fine("BP 已存在: " + name);

            // 檢查是否需要補建地址
            int locCount = DB.getSQLValue(trxName,
                "SELECT COUNT(*) FROM C_BPartner_Location WHERE C_BPartner_ID=?",
                existingId);
            if (locCount > 0) {
                log.fine("BP 已有地址，跳過: " + name);
                return bp;
            }
            // 繼續建立地址
        } else {
            // 建立新 BP
            bp = new MBPartner(ctx, 0, trxName);
            bp.setAD_Org_ID(0);
            bp.setValue(value);
            bp.setName(name);
            bp.setDescription(description);
            bp.setIsCustomer(isCustomer);
            bp.setIsVendor(isVendor);
            bp.setIsEmployee(isEmployee);

            // 設定統一編號
            if (taxId != null && !taxId.isEmpty()) {
                bp.setTaxID(taxId);
            }

            // 設定 BP 群組
            MBPGroup group = groupMap.get(groupValue);
            if (group != null) {
                bp.setC_BP_Group_ID(group.getC_BP_Group_ID());
            }

            // 設定付款條件（如果已建立）
            int paymentTermId = SamplePaymentTermSetup.getNet30PaymentTermId();
            if (paymentTermId > 0) {
                if (isCustomer) {
                    bp.setC_PaymentTerm_ID(paymentTermId);
                }
                if (isVendor) {
                    bp.setPO_PaymentTerm_ID(paymentTermId);
                }
            }

            if (!bp.save()) {
                log.severe("無法儲存 BP: " + name);
                return null;
            }

            bpMap.put(value, bp);
            isNew = true;
            log.fine("已建立 BP: " + value + " - " + name);
        }

        // 建立地址
        if (address != null && !address.isEmpty()) {
            if (!createBPLocation(ctx, bp, address, postal, city, trxName)) {
                log.warning("無法建立 BP 地址: " + name);
            }
        }

        return bp;
    }

    /**
     * 建立 BP 地址
     *
     * @param ctx 系統上下文
     * @param bp Business Partner
     * @param address 地址
     * @param postal 郵遞區號
     * @param city 城市
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    private static boolean createBPLocation(Properties ctx, MBPartner bp,
            String address, String postal, String city, String trxName) {

        try {
            // 建立地址（MLocation）
            MLocation loc = new MLocation(ctx, 0, trxName);
            loc.setC_Country_ID(taiwanCountryId);
            loc.setAddress1(address);
            loc.setCity(city);
            loc.setPostal(postal);

            if (!loc.save()) {
                log.warning("無法儲存地址: " + address);
                return false;
            }

            // 建立 BP 地址連結（MBPartnerLocation）
            MBPartnerLocation bpLoc = new MBPartnerLocation(bp);
            bpLoc.setC_Location_ID(loc.getC_Location_ID());
            bpLoc.setName(city);
            bpLoc.setIsBillTo(true);
            bpLoc.setIsShipTo(true);
            bpLoc.setIsPayFrom(true);
            bpLoc.setIsRemitTo(true);

            if (!bpLoc.save()) {
                log.warning("無法儲存 BP 地址連結: " + bp.getName());
                return false;
            }

            log.fine("已建立地址: " + bp.getName() + " -> " + address);
            return true;

        } catch (Exception e) {
            log.warning("建立 BP 地址時發生錯誤: " + e.getMessage());
            return false;
        }
    }

    /**
     * 取得 BP 對照表
     */
    public static Map<String, MBPartner> getBPMap() {
        return new HashMap<>(bpMap);
    }

    /**
     * 取得 BP 群組對照表
     */
    public static Map<String, MBPGroup> getGroupMap() {
        return new HashMap<>(groupMap);
    }

    /**
     * 取得指定的 Business Partner
     *
     * @param value BP 代碼
     * @return MBPartner 物件，不存在時回傳 null
     */
    public static MBPartner getBPartner(String value) {
        return bpMap.get(value);
    }
}
