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
import org.compiere.util.DB;
import org.compiere.util.Env;

import tw.idempiere.sample.data.OrganizationData;
import tw.idempiere.sample.util.SetupLog;

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
        SetupLog.log("OrgSetup", "開始建立組織架構，Client=" + clientId);

        // 清空對照表
        orgIdMap.clear();
        warehouseIdMap.clear();

        try {
            // 步驟 1：建立所有組織
            SetupLog.log("OrgSetup", "步驟1: 建立組織...");
            if (!createAllOrganizations(ctx, clientId, trxName)) {
                SetupLog.log("OrgSetup錯誤", "建立組織失敗");
                return false;
            }
            SetupLog.log("OrgSetup", "步驟1完成，組織數=" + orgIdMap.size());

            // 步驟 2：建立所有倉庫
            SetupLog.log("OrgSetup", "步驟2: 建立倉庫...");
            if (!createAllWarehouses(ctx, clientId, trxName)) {
                SetupLog.log("OrgSetup錯誤", "建立倉庫失敗");
                return false;
            }
            SetupLog.log("OrgSetup", "步驟2完成，倉庫數=" + warehouseIdMap.size());

            // 步驟 3：跳過儲位建立（避免交易 abort 問題）
            // 儲位可以稍後在 WebUI 中手動建立
            SetupLog.log("OrgSetup", "步驟3: 跳過儲位建立（將使用倉庫預設儲位）");

            log.info("組織架構建立完成");
            return true;

        } catch (Exception e) {
            log.severe("建立組織架構時發生錯誤: " + e.getMessage());
            SetupLog.logError("OrgSetup錯誤", "建立組織架構時發生異常", e);
            return false;
        }
    }

    /**
     * 建立所有組織
     * <p>
     * 注意：為了避免 MOrg.afterSave() 自動建立角色時發生錯誤，
     * 先以 Summary 組織方式建立（不會觸發角色建立），
     * 然後再更新為非 Summary 組織。
     * </p>
     * <p>
     * 如果組織已存在（例如 HQ 已被 MSetup 建立），會跳過並記錄到對照表。
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    private static boolean createAllOrganizations(Properties ctx, int clientId, String trxName) {
        log.info("建立組織...");

        // 記錄需要更新為非 Summary 的組織
        java.util.List<MOrg> nonSummaryOrgs = new java.util.ArrayList<>();

        for (String[] orgData : OrganizationData.ORGANIZATIONS) {
            String orgValue = orgData[OrganizationData.ORG_VALUE];
            String orgName = orgData[OrganizationData.ORG_NAME];
            String orgDesc = orgData[OrganizationData.ORG_DESC];
            boolean actualIsSummary = "Y".equals(orgData[OrganizationData.ORG_IS_SUMMARY]);

            // 檢查組織是否已存在（MSetup 可能已建立 HQ）
            int existingOrgId = DB.getSQLValue(trxName,
                "SELECT AD_Org_ID FROM AD_Org WHERE AD_Client_ID=? AND Value=?",
                clientId, orgValue);

            if (existingOrgId > 0) {
                // 組織已存在，記錄到對照表並跳過
                orgIdMap.put(orgValue, existingOrgId);
                log.info("組織已存在，跳過建立: " + orgName + " (ID=" + existingOrgId + ")");
                continue;
            }

            // 建立乾淨的 Context，確保不受其他代碼影響
            Properties cleanCtx = new Properties();
            cleanCtx.putAll(ctx);
            Env.setContext(cleanCtx, Env.AD_CLIENT_ID, clientId);
            Env.setContext(cleanCtx, Env.AD_ORG_ID, 0);
            Env.setContext(cleanCtx, Env.AD_USER_ID, 0);

            // 建立組織
            // 注意：先設定為 Summary=true 以避免 afterSave() 自動建立角色
            MOrg org = new MOrg(cleanCtx, 0, trxName);
            org.setValue(orgValue);
            org.setName(orgName);
            org.setDescription(orgDesc);
            org.setIsSummary(true); // 先設為 Summary，避免自動建立角色

            if (!org.save()) {
                log.severe("無法建立組織: " + orgName);
                SetupLog.log("Org建立失敗", orgValue + " - " + orgName);
                return false;
            }

            // 記錄組織 ID
            orgIdMap.put(orgValue, org.getAD_Org_ID());
            log.info("已建立組織: " + orgName + " (ID=" + org.getAD_Org_ID() + ")");

            // 如果實際上不是 Summary 組織，稍後需要更新
            if (!actualIsSummary) {
                nonSummaryOrgs.add(org);
            }

            // 建立組織資訊 (MOrgInfo 會在 MOrg.save() 時自動建立)
            // 但我們可以更新額外的資訊
            MOrgInfo orgInfo = MOrgInfo.get(ctx, org.getAD_Org_ID(), trxName);
            if (orgInfo != null) {
                // 可在此設定組織額外資訊，如地址等
                orgInfo.save();
            }
        }

        // 更新非 Summary 組織（使用 SQL 直接更新以避免觸發 afterSave）
        for (MOrg orgToUpdate : nonSummaryOrgs) {
            String sql = "UPDATE AD_Org SET IsSummary='N' WHERE AD_Org_ID=?";
            int updated = DB.executeUpdate(sql, orgToUpdate.getAD_Org_ID(), trxName);
            if (updated != 1) {
                log.warning("無法更新組織 IsSummary: " + orgToUpdate.getName());
            } else {
                log.fine("已更新組織為非 Summary: " + orgToUpdate.getName());
            }
        }

        return true;
    }

    /**
     * 建立所有倉庫
     * <p>
     * 每個倉庫需要一個 Location（地址），這裡為每個倉庫建立一個簡單的地址。
     * 使用 SQL 直接建立 Location 以避免 MLocation 構造函數的預設國家問題。
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    private static boolean createAllWarehouses(Properties ctx, int clientId, String trxName) {
        log.info("建立倉庫...");

        // 取得國家 ID（先嘗試台灣，再嘗試任何國家）
        int countryId = getCountryId("TW", trxName);
        if (countryId <= 0) {
            countryId = getAnyCountryId(trxName);
        }
        if (countryId <= 0) {
            log.severe("找不到任何國家資料，無法建立倉庫");
            return false;
        }
        log.info("使用國家 ID: " + countryId);

        for (String[] whData : OrganizationData.WAREHOUSES) {
            String whValue = whData[OrganizationData.WH_VALUE];
            String whName = whData[OrganizationData.WH_NAME];
            String whDesc = whData[OrganizationData.WH_DESC];
            String orgValue = whData[OrganizationData.WH_ORG];

            // 取得所屬組織 ID
            Integer orgId = orgIdMap.get(orgValue);
            if (orgId == null) {
                log.severe("找不到組織: " + orgValue + "，無法建立倉庫: " + whName);
                SetupLog.log("倉庫錯誤", "找不到組織 " + orgValue + "，orgIdMap=" + orgIdMap);
                return false;
            }

            // 使用 SQL 建立 Location（避免 MLocation 構造函數的問題）
            int locationId = createLocationBySQL(ctx, clientId, orgId, countryId, "台北市", whName + " 地址", trxName);
            if (locationId <= 0) {
                log.severe("無法建立倉庫地址: " + whName);
                return false;
            }

            // 建立乾淨的 Context
            Properties cleanCtx = new Properties();
            cleanCtx.putAll(ctx);
            Env.setContext(cleanCtx, Env.AD_CLIENT_ID, clientId);
            Env.setContext(cleanCtx, Env.AD_ORG_ID, orgId);
            Env.setContext(cleanCtx, Env.AD_USER_ID, 0);

            // 建立倉庫
            MWarehouse warehouse = new MWarehouse(cleanCtx, 0, trxName);
            warehouse.setAD_Org_ID(orgId);
            warehouse.setValue(whValue);
            warehouse.setName(whName);
            warehouse.setDescription(whDesc);
            warehouse.setC_Location_ID(locationId);

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
     * 取得國家 ID
     *
     * @param countryCode 國家代碼（如 TW, US）
     * @param trxName 交易名稱
     * @return 國家 ID，找不到時回傳 0
     */
    private static int getCountryId(String countryCode, String trxName) {
        String sql = "SELECT C_Country_ID FROM C_Country WHERE CountryCode = ?";
        return DB.getSQLValue(trxName, sql, countryCode);
    }

    /**
     * 取得任意國家 ID（作為備用）
     *
     * @param trxName 交易名稱
     * @return 國家 ID，找不到時回傳 0
     */
    private static int getAnyCountryId(String trxName) {
        String sql = "SELECT MIN(C_Country_ID) FROM C_Country WHERE IsActive='Y'";
        return DB.getSQLValue(trxName, sql);
    }

    /**
     * 使用 SQL 直接建立 Location
     * <p>
     * 避免使用 MLocation 構造函數，因為它會嘗試取得預設國家，
     * 在新 Client 建立過程中可能會失敗。
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param orgId 組織 ID
     * @param countryId 國家 ID
     * @param city 城市
     * @param address1 地址
     * @param trxName 交易名稱
     * @return Location ID，失敗時回傳 0
     */
    private static int createLocationBySQL(Properties ctx, int clientId, int orgId,
            int countryId, String city, String address1, String trxName) {
        // 取得下一個 ID
        int locationId = DB.getNextID(clientId, "C_Location", trxName);
        if (locationId <= 0) {
            return 0;
        }

        // 產生 UUID
        String uuid = java.util.UUID.randomUUID().toString();

        // 使用 SQL INSERT
        String sql = "INSERT INTO C_Location " +
                "(C_Location_ID, AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy, " +
                "C_Country_ID, City, Address1, C_Location_UU) " +
                "VALUES (?, ?, ?, 'Y', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0, ?, ?, ?, ?)";

        int result = DB.executeUpdate(sql, new Object[]{locationId, clientId, orgId, countryId, city, address1, uuid}, false, trxName);

        if (result == 1) {
            log.fine("已建立 Location ID: " + locationId);
            return locationId;
        } else {
            log.severe("無法建立 Location");
            return 0;
        }
    }

    /**
     * 建立所有儲位
     * <p>
     * 使用 SAVEPOINT 來隔離每個儲位的建立錯誤，避免單個失敗污染整個交易。
     * </p>
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param trxName 交易名稱
     * @return true=成功（至少建立了部分儲位）, false=完全失敗
     */
    private static boolean createAllLocators(Properties ctx, int clientId, String trxName) {
        log.info("建立儲位...");

        int successCount = 0;
        int failCount = 0;

        for (String[] locData : OrganizationData.LOCATORS) {
            String locValue = locData[OrganizationData.LOC_VALUE];
            String locName = locData[OrganizationData.LOC_NAME];
            String whValue = locData[OrganizationData.LOC_WAREHOUSE];
            boolean isDefault = "Y".equals(locData[OrganizationData.LOC_IS_DEFAULT]);

            // 取得所屬倉庫 ID
            Integer warehouseId = warehouseIdMap.get(whValue);
            if (warehouseId == null) {
                log.warning("找不到倉庫: " + whValue + "，跳過儲位: " + locName);
                failCount++;
                continue;
            }

            // 使用 SAVEPOINT 隔離錯誤，防止單個失敗污染整個交易
            java.sql.Savepoint savepoint = null;
            try {
                org.compiere.util.Trx trx = org.compiere.util.Trx.get(trxName, false);
                if (trx != null && trx.getConnection() != null) {
                    savepoint = trx.getConnection().setSavepoint("locator_" + locValue);
                }

                // 取得倉庫所屬組織
                MWarehouse warehouse = new MWarehouse(ctx, warehouseId, trxName);
                int orgId = warehouse.getAD_Org_ID();

                // 建立乾淨的 Context
                Properties cleanCtx = new Properties();
                cleanCtx.putAll(ctx);
                Env.setContext(cleanCtx, Env.AD_CLIENT_ID, clientId);
                Env.setContext(cleanCtx, Env.AD_ORG_ID, orgId);
                Env.setContext(cleanCtx, Env.AD_USER_ID, 0);

                // 建立儲位
                MWarehouse cleanWarehouse = new MWarehouse(cleanCtx, warehouseId, trxName);
                MLocator locator = new MLocator(cleanWarehouse, locValue);
                locator.setAD_Org_ID(orgId);
                locator.setValue(locValue);
                locator.setIsDefault(isDefault);

                if (locator.save()) {
                    log.fine("已建立儲位: " + locName + " (預設=" + isDefault + ")");
                    successCount++;
                    // 釋放 savepoint（成功時）
                    if (savepoint != null && trx != null && trx.getConnection() != null) {
                        trx.getConnection().releaseSavepoint(savepoint);
                    }
                } else {
                    // 儲存失敗，回滾到 savepoint
                    log.warning("無法建立儲位: " + locName);
                    if (savepoint != null && trx != null && trx.getConnection() != null) {
                        trx.getConnection().rollback(savepoint);
                    }
                    failCount++;
                }
            } catch (Exception e) {
                // 發生異常，回滾到 savepoint
                log.warning("建立儲位時發生錯誤: " + locName + " - " + e.getMessage());
                try {
                    org.compiere.util.Trx trx = org.compiere.util.Trx.get(trxName, false);
                    if (savepoint != null && trx != null && trx.getConnection() != null) {
                        trx.getConnection().rollback(savepoint);
                    }
                } catch (Exception rollbackEx) {
                    log.warning("回滾 savepoint 失敗: " + rollbackEx.getMessage());
                }
                failCount++;
            }
        }

        SetupLog.log("儲位結果", "成功=" + successCount + ", 失敗=" + failCount);
        log.info("儲位建立完成: 成功=" + successCount + ", 失敗=" + failCount);

        // 只要有成功建立的儲位就回傳 true，避免影響後續步驟
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
