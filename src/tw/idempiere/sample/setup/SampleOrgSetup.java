/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.compiere.model.MLocator;
import org.compiere.model.MOrg;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MWarehouse;
import org.compiere.util.CLogger;

import tw.idempiere.sample.data.OrganizationData;

/**
 * 組織和倉庫建立
 * <p>
 * 負責建立示範公司的組織架構和倉庫配置。
 * 包含組織（MOrg）、組織資訊（MOrgInfo）、倉庫（MWarehouse）和儲位（MLocator）的建立。
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class SampleOrgSetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleOrgSetup.class);

    /** 組織代碼對應 ID 的對照表 */
    private static Map<String, Integer> orgIdMap = new HashMap<>();

    /** 倉庫代碼對應 ID 的對照表 */
    private static Map<String, Integer> warehouseIdMap = new HashMap<>();

    /** 私有建構子，防止實例化 */
    private SampleOrgSetup() {
        // 工具類別，不需要實例化
    }

    /**
     * 建立所有組織和倉庫
     * <p>
     * 建立流程：
     * <ol>
     *   <li>建立所有組織及組織資訊</li>
     *   <li>建立所有倉庫</li>
     *   <li>建立所有儲位</li>
     * </ol>
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    public static boolean createOrganizations(Properties ctx, int clientId, String trxName) {
        log.info("開始建立組織架構...");

        // 清空對照表
        orgIdMap.clear();
        warehouseIdMap.clear();

        try {
            // 步驟 1：建立所有組織
            if (!createAllOrganizations(ctx, clientId, trxName)) {
                return false;
            }

            // 步驟 2：建立所有倉庫
            if (!createAllWarehouses(ctx, clientId, trxName)) {
                return false;
            }

            // 步驟 3：建立所有儲位
            if (!createAllLocators(ctx, clientId, trxName)) {
                return false;
            }

            log.info("組織架構建立完成");
            return true;

        } catch (Exception e) {
            log.severe("建立組織架構時發生錯誤: " + e.getMessage());
            return false;
        }
    }

    /**
     * 建立所有組織
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    private static boolean createAllOrganizations(Properties ctx, int clientId, String trxName) {
        log.info("建立組織...");

        for (String[] orgData : OrganizationData.ORGANIZATIONS) {
            String orgValue = orgData[OrganizationData.ORG_VALUE];
            String orgName = orgData[OrganizationData.ORG_NAME];
            String orgDesc = orgData[OrganizationData.ORG_DESC];
            boolean isSummary = "Y".equals(orgData[OrganizationData.ORG_IS_SUMMARY]);

            // 建立組織
            MOrg org = new MOrg(ctx, 0, trxName);
            org.setAD_Client_ID(clientId);
            org.setValue(orgValue);
            org.setName(orgName);
            org.setDescription(orgDesc);
            org.setIsSummary(isSummary);

            if (!org.save()) {
                log.severe("無法建立組織: " + orgName);
                return false;
            }

            // 記錄組織 ID
            orgIdMap.put(orgValue, org.getAD_Org_ID());
            log.info("已建立組織: " + orgName + " (ID=" + org.getAD_Org_ID() + ")");

            // 建立組織資訊 (MOrgInfo 會在 MOrg.save() 時自動建立)
            // 但我們可以更新額外的資訊
            MOrgInfo orgInfo = MOrgInfo.get(ctx, org.getAD_Org_ID(), trxName);
            if (orgInfo != null) {
                // 可在此設定組織額外資訊，如地址等
                orgInfo.save();
            }
        }

        return true;
    }

    /**
     * 建立所有倉庫
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    private static boolean createAllWarehouses(Properties ctx, int clientId, String trxName) {
        log.info("建立倉庫...");

        for (String[] whData : OrganizationData.WAREHOUSES) {
            String whValue = whData[OrganizationData.WH_VALUE];
            String whName = whData[OrganizationData.WH_NAME];
            String whDesc = whData[OrganizationData.WH_DESC];
            String orgValue = whData[OrganizationData.WH_ORG];

            // 取得所屬組織 ID
            Integer orgId = orgIdMap.get(orgValue);
            if (orgId == null) {
                log.severe("找不到組織: " + orgValue + "，無法建立倉庫: " + whName);
                return false;
            }

            // 建立倉庫
            MWarehouse warehouse = new MWarehouse(ctx, 0, trxName);
            warehouse.setAD_Client_ID(clientId);
            warehouse.setAD_Org_ID(orgId);
            warehouse.setValue(whValue);
            warehouse.setName(whName);
            warehouse.setDescription(whDesc);

            if (!warehouse.save()) {
                log.severe("無法建立倉庫: " + whName);
                return false;
            }

            // 記錄倉庫 ID
            warehouseIdMap.put(whValue, warehouse.getM_Warehouse_ID());
            log.info("已建立倉庫: " + whName + " (ID=" + warehouse.getM_Warehouse_ID() + ")");
        }

        return true;
    }

    /**
     * 建立所有儲位
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    private static boolean createAllLocators(Properties ctx, int clientId, String trxName) {
        log.info("建立儲位...");

        for (String[] locData : OrganizationData.LOCATORS) {
            String locValue = locData[OrganizationData.LOC_VALUE];
            String locName = locData[OrganizationData.LOC_NAME];
            String whValue = locData[OrganizationData.LOC_WAREHOUSE];
            boolean isDefault = "Y".equals(locData[OrganizationData.LOC_IS_DEFAULT]);

            // 取得所屬倉庫 ID
            Integer warehouseId = warehouseIdMap.get(whValue);
            if (warehouseId == null) {
                log.severe("找不到倉庫: " + whValue + "，無法建立儲位: " + locName);
                return false;
            }

            // 取得倉庫所屬組織
            MWarehouse warehouse = new MWarehouse(ctx, warehouseId, trxName);
            int orgId = warehouse.getAD_Org_ID();

            // 建立儲位
            MLocator locator = new MLocator(warehouse, locValue);
            locator.setAD_Client_ID(clientId);
            locator.setAD_Org_ID(orgId);
            locator.setValue(locValue);
            locator.setIsDefault(isDefault);

            if (!locator.save()) {
                log.severe("無法建立儲位: " + locName);
                return false;
            }

            log.fine("已建立儲位: " + locName + " (預設=" + isDefault + ")");
        }

        log.info("已建立所有儲位");
        return true;
    }

    /**
     * 取得組織 ID
     *
     * @param orgValue 組織代碼
     * @return 組織 ID，找不到時回傳 -1
     */
    public static int getOrgId(String orgValue) {
        Integer id = orgIdMap.get(orgValue);
        return id != null ? id : -1;
    }

    /**
     * 取得倉庫 ID
     *
     * @param whValue 倉庫代碼
     * @return 倉庫 ID，找不到時回傳 -1
     */
    public static int getWarehouseId(String whValue) {
        Integer id = warehouseIdMap.get(whValue);
        return id != null ? id : -1;
    }

    /**
     * 取得所有組織 ID 對照表
     *
     * @return 組織代碼對應 ID 的 Map
     */
    public static Map<String, Integer> getOrgIdMap() {
        return new HashMap<>(orgIdMap);
    }

    /**
     * 取得所有倉庫 ID 對照表
     *
     * @return 倉庫代碼對應 ID 的 Map
     */
    public static Map<String, Integer> getWarehouseIdMap() {
        return new HashMap<>(warehouseIdMap);
    }
}
