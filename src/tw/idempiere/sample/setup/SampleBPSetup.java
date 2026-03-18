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
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import tw.idempiere.sample.data.BPartnerData;

/**
 * Business Partner 建立
 * <p>
 * 負責建立示範公司的業務夥伴資料，包含：
 * <ul>
 *   <li>BP 群組（供應商、客戶、員工）</li>
 *   <li>供應商（5 筆）</li>
 *   <li>客戶（7 筆）</li>
 *   <li>員工（3 筆）</li>
 * </ul>
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class SampleBPSetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleBPSetup.class);

    /** BP 群組對照表（Value -> MBPGroup） */
    private static Map<String, MBPGroup> groupMap = new HashMap<>();

    /** BP 對照表（Value -> MBPartner） */
    private static Map<String, MBPartner> bpMap = new HashMap<>();

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

        // 清空對照表
        groupMap.clear();
        bpMap.clear();

        try {
            // 步驟 1：建立 BP 群組
            if (!createBPGroups(ctx, clientId, trxName)) {
                log.severe("無法建立 BP 群組");
                return false;
            }

            // 步驟 2：建立供應商（5 筆）
            for (String[] data : BPartnerData.VENDORS) {
                MBPartner bp = createBPartner(ctx, clientId, data[0], data[1], data[2],
                        false, true, false, "VENDOR", trxName);
                if (bp == null) {
                    log.warning("無法建立供應商: " + data[1] + "，繼續處理其他資料");
                }
            }

            // 步驟 3：建立客戶（7 筆）
            for (String[] data : BPartnerData.CUSTOMERS) {
                MBPartner bp = createBPartner(ctx, clientId, data[0], data[1], data[2],
                        true, false, false, "CUSTOMER", trxName);
                if (bp == null) {
                    log.warning("無法建立客戶: " + data[1] + "，繼續處理其他資料");
                }
            }

            // 步驟 4：建立員工（3 筆）
            for (String[] data : BPartnerData.EMPLOYEES) {
                MBPartner bp = createBPartner(ctx, clientId, data[0], data[1], data[2],
                        false, false, true, "EMPLOYEE", trxName);
                if (bp == null) {
                    log.warning("無法建立員工: " + data[1] + "，繼續處理其他資料");
                }
            }

            log.info("Business Partners 建立完成，共 " + bpMap.size() + " 筆");
            return true;

        } catch (Exception e) {
            log.severe("建立 Business Partners 時發生錯誤: " + e.getMessage());
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
     * 建立單一 Business Partner
     */
    private static MBPartner createBPartner(Properties ctx, int clientId, String value,
            String name, String description, boolean isCustomer, boolean isVendor,
            boolean isEmployee, String groupValue, String trxName) {

        // 檢查是否已存在
        int existingId = DB.getSQLValue(trxName,
            "SELECT C_BPartner_ID FROM C_BPartner WHERE AD_Client_ID=? AND Value=?",
            clientId, value);

        if (existingId > 0) {
            MBPartner existing = new MBPartner(ctx, existingId, trxName);
            bpMap.put(value, existing);
            log.fine("BP 已存在，跳過: " + name);
            return existing;
        }

        // 取得 BP 群組
        MBPGroup group = groupMap.get(groupValue);

        MBPartner bp = new MBPartner(ctx, 0, trxName);
        bp.setAD_Org_ID(0);
        bp.setValue(value);
        bp.setName(name);
        bp.setDescription(description);
        bp.setIsCustomer(isCustomer);
        bp.setIsVendor(isVendor);
        bp.setIsEmployee(isEmployee);

        if (group != null) {
            bp.setC_BP_Group_ID(group.getC_BP_Group_ID());
        }

        if (!bp.save()) {
            log.severe("無法儲存 BP: " + name);
            return null;
        }

        bpMap.put(value, bp);
        log.fine("已建立 BP: " + value + " - " + name);
        return bp;
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
}
