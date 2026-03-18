/******************************************************************************
 * 天地人實業有限公司 - iDempiere 台灣示範資料插件
 * Copyright (C) 天地人實業有限公司. All Rights Reserved.
 * 授權條款：GNU General Public License version 2
 *****************************************************************************/
package tw.idempiere.sample.setup;

import java.util.Properties;

import org.compiere.model.MClient;
import org.compiere.model.MClientInfo;
import org.compiere.model.MRole;
import org.compiere.model.MUser;
import org.compiere.model.MUserRoles;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Trx;

/**
 * Client 建立主流程控制類別
 * <p>
 * 負責建立示範 Client（天地人實業有限公司）及其所有相關資料。
 * 包含 Client、組織、會計年度、角色、用戶等的建立。
 * </p>
 * <p>
 * 使用方式：
 * <pre>
 * SampleClientSetup.init();
 * </pre>
 * </p>
 *
 * @author 天地人實業
 * @version 1.0
 */
public class SampleClientSetup {

    /** 日誌記錄器 */
    private static final CLogger log = CLogger.getCLogger(SampleClientSetup.class);

    /** Client 的 Search Key（唯一識別碼） */
    public static final String CLIENT_VALUE = "sample";

    /** Client 名稱 */
    public static final String CLIENT_NAME = "天地人實業有限公司";

    /** 管理員角色名稱 */
    private static final String ADMIN_ROLE_NAME = "天地人管理員";

    /** 管理員用戶名稱 */
    private static final String ADMIN_USER_NAME = "SampleAdmin";

    /** 管理員用戶密碼（預設密碼，應在首次登入後更改） */
    private static final String ADMIN_USER_PASSWORD = "Sample123!";

    /** 私有建構子，防止實例化 */
    private SampleClientSetup() {
        // 工具類別，不需要實例化
    }

    /**
     * 檢查 Sample Client 是否已存在
     *
     * @return true=存在, false=不存在
     */
    public static boolean clientExists() {
        return getExistingClient() != null;
    }

    /**
     * 取得現有的 Sample Client
     *
     * @return MClient 物件，不存在時回傳 null
     */
    public static MClient getExistingClient() {
        Properties ctx = Env.getCtx();
        return new Query(ctx, MClient.Table_Name, "Value=?", null)
                .setParameters(CLIENT_VALUE)
                .setOnlyActiveRecords(true)
                .first();
    }

    /**
     * 執行完整的初始化流程
     * <p>
     * 初始化流程：
     * <ol>
     *   <li>檢查 Client 是否已存在，存在則跳過</li>
     *   <li>建立 Transaction</li>
     *   <li>呼叫 createClient() 建立所有資料</li>
     *   <li>成功時 Commit，失敗時 Rollback</li>
     * </ol>
     * </p>
     *
     * @return true=成功（包含已存在的情況）, false=失敗
     */
    public static boolean init() {
        log.info("開始初始化 Sample Client...");

        // 檢查是否已存在
        if (clientExists()) {
            log.info("Sample Client 已存在，跳過初始化");
            return true;
        }

        // 建立交易
        String trxName = Trx.createTrxName("SampleSetup");
        Trx trx = Trx.get(trxName, true);

        boolean success = false;
        try {
            // 執行建立流程
            success = createClient(Env.getCtx(), trxName);

            if (success) {
                trx.commit();
                log.info("Sample Client 初始化完成");
            } else {
                trx.rollback();
                log.severe("Sample Client 初始化失敗，已回滾");
            }
        } catch (Exception e) {
            log.severe("初始化時發生錯誤: " + e.getMessage());
            trx.rollback();
            success = false;
        } finally {
            trx.close();
        }

        return success;
    }

    /**
     * 建立 Client 及所有相關資料
     * <p>
     * 建立流程：
     * <ol>
     *   <li>建立 MClient</li>
     *   <li>建立 MClientInfo</li>
     *   <li>呼叫 SampleOrgSetup.createOrganizations() 建立組織和倉庫</li>
     *   <li>呼叫 SampleCalendarSetup.createCalendar() 建立會計年度</li>
     *   <li>建立管理員角色</li>
     *   <li>建立管理員用戶</li>
     *   <li>（後續 Task 會加入更多呼叫）</li>
     * </ol>
     * </p>
     *
     * @param ctx 系統上下文
     * @param trxName 交易名稱
     * @return true=成功, false=失敗
     */
    public static boolean createClient(Properties ctx, String trxName) {
        log.info("開始建立 Client: " + CLIENT_NAME);

        try {
            // 步驟 1：建立 MClient
            MClient client = createMClient(ctx, trxName);
            if (client == null) {
                return false;
            }

            int clientId = client.getAD_Client_ID();

            // 設定上下文中的 Client ID
            Env.setContext(ctx, Env.AD_CLIENT_ID, clientId);

            // 步驟 2：建立組織和倉庫
            log.info("建立組織和倉庫...");
            if (!SampleOrgSetup.createOrganizations(ctx, clientId, trxName)) {
                log.severe("建立組織失敗");
                return false;
            }

            // 取得總公司組織 ID
            int hqOrgId = SampleOrgSetup.getOrgId("HQ");
            if (hqOrgId <= 0) {
                log.severe("無法取得總公司組織 ID");
                return false;
            }

            // 步驟 3：建立會計年度和帳期
            log.info("建立會計年度...");
            if (SampleCalendarSetup.createCalendar(ctx, clientId, 0, trxName) == null) {
                log.severe("建立會計年度失敗");
                return false;
            }

            // 步驟 4：建立管理員角色
            log.info("建立管理員角色...");
            MRole adminRole = createAdminRole(ctx, clientId, hqOrgId, trxName);
            if (adminRole == null) {
                log.severe("建立管理員角色失敗");
                return false;
            }

            // 步驟 5：建立管理員用戶
            log.info("建立管理員用戶...");
            MUser adminUser = createAdminUser(ctx, clientId, hqOrgId, adminRole, trxName);
            if (adminUser == null) {
                log.severe("建立管理員用戶失敗");
                return false;
            }

            log.info("Client 建立完成: " + CLIENT_NAME);
            return true;

        } catch (Exception e) {
            log.severe("建立 Client 時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 建立 MClient
     *
     * @param ctx 系統上下文
     * @param trxName 交易名稱
     * @return 建立的 MClient，失敗時回傳 null
     */
    private static MClient createMClient(Properties ctx, String trxName) {
        // 建立 Client
        MClient client = new MClient(ctx, 0, true, trxName);
        client.setValue(CLIENT_VALUE);
        client.setName(CLIENT_NAME);
        client.setDescription("iDempiere 台灣示範公司");

        if (!client.save()) {
            log.severe("無法建立 Client: " + CLIENT_NAME);
            return null;
        }

        log.info("已建立 Client: " + CLIENT_NAME + " (ID=" + client.getAD_Client_ID() + ")");

        // MClientInfo 會在 MClient 建立時自動建立
        // 取得並更新額外資訊
        MClientInfo clientInfo = MClientInfo.get(ctx, client.getAD_Client_ID(), trxName);
        if (clientInfo != null) {
            // 設定 Automatic Period Control = false（手動控制帳期）
            // clientInfo.setIsAutoArchive(false); // 如果需要的話
            clientInfo.save();
        }

        return client;
    }

    /**
     * 建立管理員角色
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param orgId 組織 ID
     * @param trxName 交易名稱
     * @return 建立的 MRole，失敗時回傳 null
     */
    private static MRole createAdminRole(Properties ctx, int clientId, int orgId, String trxName) {
        MRole role = new MRole(ctx, 0, trxName);
        role.setAD_Client_ID(clientId);
        role.setAD_Org_ID(0); // 角色通常設定在 Client 層級
        role.setName(ADMIN_ROLE_NAME);
        role.setDescription("天地人實業管理員角色，擁有完整權限");
        role.setUserLevel(MRole.USERLEVEL_Client); // Client 層級
        role.setIsManual(false);
        role.setIsShowAcct(true); // 顯示會計相關欄位
        role.setIsCanExport(true); // 允許匯出
        role.setIsCanReport(true); // 允許產生報表
        role.setIsAccessAllOrgs(true); // 可存取所有組織

        if (!role.save()) {
            log.severe("無法建立角色: " + ADMIN_ROLE_NAME);
            return null;
        }

        log.info("已建立角色: " + ADMIN_ROLE_NAME + " (ID=" + role.getAD_Role_ID() + ")");
        return role;
    }

    /**
     * 建立管理員用戶
     *
     * @param ctx 系統上下文
     * @param clientId Client ID
     * @param orgId 組織 ID
     * @param role 管理員角色
     * @param trxName 交易名稱
     * @return 建立的 MUser，失敗時回傳 null
     */
    private static MUser createAdminUser(Properties ctx, int clientId, int orgId,
                                         MRole role, String trxName) {
        // 建立用戶
        MUser user = new MUser(ctx, 0, trxName);
        user.setAD_Client_ID(clientId);
        user.setAD_Org_ID(orgId);
        user.setValue(ADMIN_USER_NAME);
        user.setName(ADMIN_USER_NAME);
        user.setDescription("天地人實業管理員帳號");
        user.setPassword(ADMIN_USER_PASSWORD);
        user.setIsLoginUser(true); // 可登入
        user.setIsFullBPAccess(true); // 可存取所有業務夥伴

        if (!user.save()) {
            log.severe("無法建立用戶: " + ADMIN_USER_NAME);
            return null;
        }

        log.info("已建立用戶: " + ADMIN_USER_NAME + " (ID=" + user.getAD_User_ID() + ")");

        // 建立用戶與角色的關聯
        MUserRoles userRole = new MUserRoles(ctx, user.getAD_User_ID(), role.getAD_Role_ID(), trxName);
        userRole.setAD_Client_ID(clientId);
        userRole.setAD_Org_ID(0);
        userRole.setIsActive(true);

        if (!userRole.save()) {
            log.severe("無法建立用戶角色關聯");
            return null;
        }

        log.info("已建立用戶角色關聯: " + ADMIN_USER_NAME + " -> " + ADMIN_ROLE_NAME);
        return user;
    }

    /**
     * 取得 Client ID
     * <p>
     * 用於在 Client 建立後取得 Client ID，供其他 Setup 類別使用。
     * </p>
     *
     * @return Client ID，不存在時回傳 -1
     */
    public static int getClientId() {
        MClient client = getExistingClient();
        return client != null ? client.getAD_Client_ID() : -1;
    }
}
